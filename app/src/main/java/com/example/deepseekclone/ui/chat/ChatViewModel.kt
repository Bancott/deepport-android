package com.example.deepseekclone.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepseekclone.data.ChatMessage
import com.example.deepseekclone.data.ChatRepository
import com.example.deepseekclone.data.ChatStore
import com.example.deepseekclone.data.SettingsStore
import com.example.deepseekclone.data.StreamEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 会话页 ViewModel：单向数据流（UiState → Compose），业务动作走 Repository。
 * - 流式增量在内存里累积，按 ~60ms 的时间窗刷到 StateFlow，避免每个 delta 都触发整表重组
 * - 停止 = 取消 Job，消息落到 Aborted
 */
class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore.get(app)
    private val chatStore = ChatStore(app)
    private val repository = ChatRepository()

    private val _state = MutableStateFlow(ChatUiState())

    init {
        // 恢复上次会话（杀进程重进还能看到）
        chatStore.load()?.let { (title, messages) ->
            if (messages.isNotEmpty()) {
                val total = messages.sumOf { it.usage?.total ?: 0 }
                _state.value = ChatUiState(title = title, messages = messages, sessionTokens = total)
            }
        }
    }

    /** 消息列表变化后落盘（本地持久化） */
    private fun persist() {
        val s = _state.value
        if (s.messages.isEmpty()) chatStore.clear() else chatStore.save(s.title, s.messages)
    }
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null

    val selectedModel: String get() = settings.model
    val hasApiKey: Boolean get() = settings.apiKey.isNotBlank()

    /* ----------------------------- 输入区 ----------------------------- */

    fun updateInput(text: String) = _state.update { it.copy(input = it.input.copy(text = text)) }

    fun toggleDeepThinking() = _state.update {
        it.copy(input = it.input.copy(deepThinking = !it.input.deepThinking))
    }

    fun toggleSmartSearch() = _state.update {
        it.copy(input = it.input.copy(smartSearch = !it.input.smartSearch))
    }

    fun toggleInputMode() = _state.update {
        val next = if (it.input.mode == InputMode.Text) InputMode.Voice else InputMode.Text
        it.copy(input = it.input.copy(mode = next))
    }

    fun dismissTranscribedNotice() = _state.update {
        it.copy(input = it.input.copy(transcribedNotice = null))
    }

    fun toggleSpeechOutput() = _state.update { it.copy(isSpeechOutputOn = !it.isSpeechOutputOn) }

    fun newChat() {
        chatStore.clear()
        streamJob?.cancel()
        _state.value = ChatUiState()
    }

    /* ----------------------------- 发送 / 停止 ----------------------------- */

    fun send() {
        val current = _state.value
        if (!current.input.canSend || current.isGenerating) return

        val text = current.input.text.trim()
        val userId = "u-" + UUID.randomUUID().toString().take(8)
        val assistantId = "a-" + UUID.randomUUID().toString().take(8)

        // 先把上下文快照下来（发送前的已完成消息）
        val history = current.messages
            .filter { it.status == MessageStatus.Completed || it.status == MessageStatus.Aborted }
            .map { ChatMessage(role = if (it.role == ChatRole.User) "user" else "assistant", content = it.text) }
            .plus(ChatMessage("user", text))

        _state.update {
            it.copy(
                title = it.title.ifBlank { text.take(20) },
                generation = GenerationState.Connecting,
                input = it.input.copy(text = "", transcribedNotice = null),
                messages = it.messages + listOf(
                    MessageUi(id = userId, role = ChatRole.User, text = text, status = MessageStatus.Sent),
                    MessageUi(id = assistantId, role = ChatRole.Assistant, status = MessageStatus.Connecting),
                ),
            )
        }

        val apiKey = settings.apiKey
        val baseUrl = settings.baseUrl
        val model = settings.model
        if (apiKey.isBlank() || model.isBlank()) {
            fail(assistantId, "请先在「设置 → 模型设置」填写 API Key 并选择模型")
            return
        }

        streamJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            var reasoning = StringBuilder()
            var content = StringBuilder()
            var lastFlush = 0L
            var firstContentAt = 0L
            var usage: TokenUsage? = null

            fun flush(force: Boolean = false) {
                val now = System.currentTimeMillis()
                if (!force && now - lastFlush < 60) return
                lastFlush = now
                val seconds = if (firstContentAt > 0) {
                    ((firstContentAt - startedAt) / 1000).toInt().coerceAtLeast(0)
                } else {
                    ((now - startedAt) / 1000).toInt().coerceAtLeast(0)
                }
                _state.update { st ->
                    st.copy(
                        messages = st.messages.map { m ->
                            if (m.id != assistantId) {
                                m
                            } else {
                                m.copy(
                                    reasoning = reasoning.toString(),
                                    text = content.toString(),
                                    reasoningSeconds = seconds,
                                    usage = usage,
                                )
                            }
                        },
                    )
                }
            }

            try {
                repository.streamChat(
                    baseUrl = baseUrl,
                    apiKey = apiKey,
                    model = model,
                    messages = history,
                ).collect { event ->
                    when (event) {
                        is StreamEvent.Reasoning -> {
                            if (reasoning.isEmpty()) {
                                _state.update { st -> st.updateMessage(assistantId) {
                                    it.copy(status = MessageStatus.Reasoning, reasoningExpanded = true)
                                } }
                            }
                            reasoning.append(event.delta)
                            flush()
                        }

                        is StreamEvent.Content -> {
                            if (firstContentAt == 0L) {
                                firstContentAt = System.currentTimeMillis()
                                _state.update { st ->
                                    st.copy(generation = GenerationState.Streaming)
                                        .updateMessage(assistantId) {
                                            it.copy(status = MessageStatus.Streaming, reasoningExpanded = false)
                                        }
                                }
                            }
                            content.append(event.delta)
                            flush()
                        }

                        is StreamEvent.Usage -> {
                            usage = TokenUsage(
                                prompt = event.promptTokens,
                                completion = event.completionTokens,
                                reasoning = event.reasoningTokens,
                            )
                        }

                        StreamEvent.Done -> Unit
                    }
                }
                flush(force = true)
                _state.update { st ->
                    st.copy(
                        generation = GenerationState.Idle,
                        messages = st.messages.map { m ->
                            if (m.id == assistantId) {
                                m.copy(status = MessageStatus.Completed, usage = usage)
                            } else {
                                m
                            }
                        },
                        sessionTokens = st.sessionTokens + (usage?.total ?: 0),
                    )
                }
                persist()
            } catch (e: kotlinx.coroutines.CancellationException) {
                flush(force = true)
                _state.update { st ->
                    st.copy(
                        generation = GenerationState.Idle,
                        messages = st.messages.map { m ->
                            if (m.id == assistantId) m.copy(status = MessageStatus.Aborted) else m
                        },
                    )
                }
                persist()
                throw e
            } catch (e: Exception) {
                flush(force = true)
                fail(assistantId, e.message ?: "请求失败，请稍后重试")
            }
        }
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
    }

    fun toggleReasoning(id: String) = _state.update { st ->
        st.updateMessage(id) { it.copy(reasoningExpanded = !it.reasoningExpanded) }
    }

    fun regenerate(message: MessageUi) {
        // 占位：分支树接上后替换为"新建 sibling"
        if (_state.value.isGenerating) return
    }

    private fun fail(assistantId: String, hint: String) {
        _state.update { st ->
            st.copy(
                generation = GenerationState.Failed,
                messages = st.messages.map { m ->
                    if (m.id == assistantId) {
                        m.copy(status = MessageStatus.Failed, errorHint = hint)
                    } else {
                        m
                    }
                },
            )
        }
        persist()
    }
}

private fun ChatUiState.updateMessage(
    id: String,
    block: (MessageUi) -> MessageUi,
): ChatUiState = copy(messages = messages.map { if (it.id == id) block(it) else it })




