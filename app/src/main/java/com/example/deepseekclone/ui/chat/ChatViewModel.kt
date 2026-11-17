package com.example.deepseekclone.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.deepseekclone.data.ChatMessage
import com.example.deepseekclone.data.ChatRepository
import com.example.deepseekclone.data.ChatStore
import com.example.deepseekclone.data.ConversationStore
import com.example.deepseekclone.data.SettingsStore
import com.example.deepseekclone.data.StreamEvent
import com.example.deepseekclone.data.StoredConversation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** 搜索结果：会话 + 命中片段 */
data class SearchHit(
    val conversationId: String,
    val title: String,
    val group: String,
    val snippet: String,
    val titleHit: Boolean,
)

/**
 * 会话页 ViewModel：单向数据流（UiState → Compose），业务动作走 Repository。
 *
 * 消息是**树**：`nodes` 存全部节点（含历史分支），`activeChild` 存每个父节点选了哪个子节点。
 * 每次对外只暴露 active path，并能顺便算出每个节点的 branchIndex/branchCount。
 */
class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore.get(app)
    private val store = ConversationStore(app)
    private val repository = ChatRepository()
    private var currentId: String = ""

    /** 全部消息节点（插入顺序 = 创建顺序，兄弟节点顺序即分支顺序） */
    private val nodes = LinkedHashMap<String, MessageUi>()

    /** 父节点 id → 选中的子节点 id；key "" 表示根 */
    private val activeChild = LinkedHashMap<String, String>()

    private var title: String = ""
    private var titleEdited: Boolean = false
    private var pinned: Boolean = false
    private var pinnedAt: Long = 0L

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null

    init {
        // 一次性迁移：老版本把单会话存在 ChatStore（ds_chat.xml）里
        if (store.loadAll().isEmpty()) {
            ChatStore(app).load()?.let { (oldTitle, messages) ->
                if (messages.isNotEmpty()) {
                    val chain = messages.mapIndexed { index, m ->
                        m.copy(parentId = messages.getOrNull(index - 1)?.id)
                    }
                    store.upsert(
                        StoredConversation(
                            id = store.newId(),
                            title = oldTitle.ifBlank { "新对话" },
                            updatedAt = System.currentTimeMillis(),
                            messages = chain,
                            activeChild = chain.associate { it.parentId.orEmpty() to it.id },
                        ),
                    )
                }
            }
        }
        val latest = store.loadAll().firstOrNull()
        if (latest != null) bind(latest) else currentId = store.newId()
    }

    /* ----------------------------- 会话生命周期 ----------------------------- */

    private fun bind(c: StoredConversation) {
        currentId = c.id
        title = c.title
        titleEdited = c.titleEdited
        pinned = c.pinned
        pinnedAt = c.pinnedAt
        nodes.clear()
        c.messages.forEach { nodes[it.id] = it }
        activeChild.clear()
        activeChild.putAll(c.activeChild.ifEmpty { inferActiveChild(c.messages) })
        val prev = _state.value
        _state.value = ChatUiState(
            title = c.title,
            messages = activePath(),
            sessionTokens = sessionTokens(),
            isSpeechOutputOn = prev.isSpeechOutputOn,
            input = prev.input.copy(
                text = "",
                attachments = emptyList(),
                transcribing = false,
                transcribedNotice = null,
            ),
        )
    }

    /** 打开历史会话 */
    fun open(id: String) {
        if (id == currentId) return
        streamJob?.cancel()
        streamJob = null
        val target = store.loadAll().firstOrNull { it.id == id } ?: return
        bind(target)
    }

    fun newChat() {
        streamJob?.cancel()
        streamJob = null
        currentId = store.newId()
        nodes.clear()
        activeChild.clear()
        title = ""
        titleEdited = false
        pinned = false
        pinnedAt = 0L
        val prev = _state.value
        _state.value = ChatUiState(
            isSpeechOutputOn = prev.isSpeechOutputOn,
            input = prev.input.copy(text = "", attachments = emptyList(), transcribedNotice = null),
        )
    }

    fun deleteConversation(id: String) {
        store.delete(id)
        if (id == currentId) newChat()
    }

    fun deleteConversations(ids: Collection<String>) {
        ids.forEach { store.delete(it) }
        if (ids.contains(currentId)) newChat()
    }

    fun renameConversation(id: String, newTitle: String) {
        val clean = newTitle.trim().replace('\n', ' ')
        if (clean.isEmpty()) return
        val target = store.loadAll().firstOrNull { it.id == id } ?: return
        store.upsert(target.copy(title = clean, titleEdited = true))
        if (id == currentId) {
            title = clean
            titleEdited = true
            _state.update { it.copy(title = clean) }
        }
    }

    fun togglePin(id: String) {
        val target = store.loadAll().firstOrNull { it.id == id } ?: return
        setPinned(target.copy(pinned = !target.pinned))
    }

    fun setPinned(ids: Collection<String>, pinned: Boolean) {
        val all = store.loadAll()
        all.filter { it.id in ids }.forEach { setPinned(it.copy(pinned = pinned)) }
    }

    private fun setPinned(c: StoredConversation) {
        val at = if (c.pinned) System.currentTimeMillis() else 0L
        store.upsert(c.copy(pinnedAt = at))
        if (c.id == currentId) {
            pinned = c.pinned
            pinnedAt = at
        }
    }

    /* ------------------------------- 抽屉 / 搜索 ------------------------------- */

    fun conversationGroups(): List<ConversationGroup> {
        val all = store.loadAll()
        val groups = mutableListOf<ConversationGroup>()
        val pinnedList = all.filter { it.pinned }.sortedByDescending { it.pinnedAt }
        if (pinnedList.isNotEmpty()) {
            groups += ConversationGroup("置顶", pinnedList.map { toUi(it, "置顶") })
        }
        all.filterNot { it.pinned }
            .groupBy { it.groupLabel }
            .forEach { (label, items) ->
                groups += ConversationGroup(label, items.map { toUi(it, label) })
            }
        return groups
    }

    private fun toUi(c: StoredConversation, label: String) = ConversationUi(
        id = c.id,
        title = c.title.ifBlank { "新对话" },
        group = label,
        pinned = c.pinned,
    )

    fun search(query: String): List<SearchHit> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val hits = mutableListOf<SearchHit>()
        store.loadAll().forEach { c ->
            val titleHit = c.title.contains(q, ignoreCase = true)
            val matched = c.activeMessages().firstOrNull { m ->
                m.role == ChatRole.User && m.text.contains(q, ignoreCase = true)
            } ?: c.activeMessages().firstOrNull { it.text.contains(q, ignoreCase = true) }
            if (!titleHit && matched == null) return@forEach
            val snippet = matched?.let { snippetOf(it.text, q) }
                ?: c.activeMessages().firstOrNull { it.text.isNotBlank() }?.text?.take(60).orEmpty()
            hits += SearchHit(
                conversationId = c.id,
                title = c.title.ifBlank { "新对话" },
                group = if (c.pinned) "置顶" else c.groupLabel,
                snippet = snippet,
                titleHit = titleHit,
            )
        }
        return hits.sortedByDescending { it.titleHit }
    }

    private fun snippetOf(text: String, query: String): String {
        val flat = text.replace('\n', ' ')
        val index = flat.indexOf(query, ignoreCase = true)
        if (index < 0) return flat.take(60)
        val start = (index - 24).coerceAtLeast(0)
        val end = (index + query.length + 36).coerceAtMost(flat.length)
        return buildString {
            if (start > 0) append('…')
            append(flat.substring(start, end))
            if (end < flat.length) append('…')
        }
    }

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

    fun addAttachments(items: List<AttachmentUi>) = _state.update {
        it.copy(input = it.input.copy(attachments = (it.input.attachments + items).take(9)))
    }

    fun removeAttachment(id: String) = _state.update {
        it.copy(input = it.input.copy(attachments = it.input.attachments.filterNot { a -> a.id == id }))
    }

    fun setTranscribing(value: Boolean) = _state.update {
        it.copy(input = it.input.copy(transcribing = value))
    }

    /** 语音转写结果回填：写入输入框并给出"已转为文字"提示 */
    fun applyTranscription(text: String) {
        _state.update {
            val merged = if (it.input.text.isBlank()) text else it.input.text + text
            it.copy(
                input = it.input.copy(
                    text = merged,
                    transcribing = false,
                    transcribedNotice = "🎙 已转为文字，可编辑",
                ),
            )
        }
    }

    /** 转写失败：清掉转写态并给出提示 */
    fun failTranscription(hint: String) {
        _state.update {
            it.copy(input = it.input.copy(transcribing = false, transcribedNotice = hint))
        }
    }

    /* ----------------------------- 发送 / 停止 ----------------------------- */

    fun send() {
        val current = _state.value
        if (!current.input.canSend || current.isGenerating) return

        val text = current.input.text.trim()
        val attachments = current.input.attachments
        val parentKey = activePath().lastOrNull()?.id.orEmpty()

        val userId = newId("u")
        val assistantId = newId("a")
        nodes[userId] = MessageUi(
            id = userId,
            role = ChatRole.User,
            text = text,
            status = MessageStatus.Sent,
            parentId = parentKey.ifEmpty { null },
            attachments = attachments,
        )
        nodes[assistantId] = MessageUi(
            id = assistantId,
            role = ChatRole.Assistant,
            parentId = userId,
            status = MessageStatus.Connecting,
        )
        activeChild[parentKey] = userId
        activeChild[userId] = assistantId

        if (title.isBlank()) {
            title = (text.take(20).ifBlank { attachments.firstOrNull()?.displayName?.take(20) ?: "" })
                .ifBlank { "新对话" }
        }

        _state.update {
            it.copy(
                title = title,
                generation = GenerationState.Connecting,
                input = it.input.copy(text = "", attachments = emptyList(), transcribedNotice = null),
                messages = activePath(),
                sessionTokens = sessionTokens(),
            )
        }
        startStream(assistantId, userId)
    }

    fun stop() {
        streamJob?.cancel()
        streamJob = null
    }

    /** 重新生成：在同一父节点下新建 sibling，并切到新分支 */
    fun regenerate(message: MessageUi) {
        if (_state.value.isGenerating) return
        if (message.role != ChatRole.Assistant) return
        val parentKey = message.parentId ?: return
        if (nodes[parentKey] == null) return

        val assistantId = newId("a")
        nodes[assistantId] = MessageUi(
            id = assistantId,
            role = ChatRole.Assistant,
            parentId = parentKey,
            status = MessageStatus.Connecting,
        )
        activeChild[parentKey] = assistantId
        _state.update {
            it.copy(generation = GenerationState.Connecting, messages = activePath())
        }
        startStream(assistantId, parentKey)
    }

    /** 失败重试：复用同一节点原地重跑，不产生多余分支 */
    fun retry(message: MessageUi) {
        if (_state.value.isGenerating) return
        if (message.role != ChatRole.Assistant) return
        val parentKey = message.parentId ?: return
        if (nodes[message.id] == null) return

        nodes[message.id] = message.copy(
            text = "",
            reasoning = "",
            reasoningSeconds = 0,
            errorHint = null,
            usage = null,
            status = MessageStatus.Connecting,
        )
        activeChild[parentKey] = message.id
        _state.update {
            it.copy(generation = GenerationState.Connecting, messages = activePath())
        }
        startStream(message.id, parentKey)
    }

    /** 分支切换：±1 在同父节点的兄弟之间移动，到头即停 */
    fun selectBranch(message: MessageUi, delta: Int) {
        val siblings = childrenOf(message.parentId)
        if (siblings.size < 2) return
        val index = siblings.indexOfFirst { it.id == message.id }
        if (index < 0) return
        val next = (index + delta).coerceIn(0, siblings.lastIndex)
        if (next == index) return
        activeChild[message.parentId.orEmpty()] = siblings[next].id
        _state.update { it.copy(messages = activePath(), sessionTokens = sessionTokens()) }
        persist()
    }

    fun toggleReasoning(id: String) {
        val node = nodes[id] ?: return
        nodes[id] = node.copy(reasoningExpanded = !node.reasoningExpanded)
        _state.update { it.copy(messages = activePath()) }
    }

    /* ----------------------------- 流式请求 ----------------------------- */

    private fun startStream(assistantId: String, parentKey: String) {
        val apiKey = settings.apiKey
        val model = settings.model
        val baseUrl = settings.baseUrl
        if (apiKey.isBlank() || model.isBlank()) {
            fail(assistantId, "请先在「设置 → 模型设置」填写 API Key 并选择模型")
            return
        }
        val history = historyTo(parentKey).map {
            ChatMessage(role = if (it.role == ChatRole.User) "user" else "assistant", content = promptFor(it))
        }
        val effort = if (_state.value.input.deepThinking) "max" else "low"

        streamJob = viewModelScope.launch {
            streamInto(assistantId, baseUrl, apiKey, model, history, effort)
        }
    }

    private suspend fun streamInto(
        assistantId: String,
        baseUrl: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        effort: String,
    ) {
        val startedAt = System.currentTimeMillis()
        val reasoning = StringBuilder()
        val content = StringBuilder()
        var lastFlush = 0L
        var firstContentAt = 0L
        var usage: TokenUsage? = null

        fun flush(force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (!force && now - lastFlush < 60) return
            lastFlush = now
            val elapsed = if (firstContentAt > 0) firstContentAt - startedAt else now - startedAt
            // 推理很快时也至少显示 1 秒，避免出现"用时 0 秒"
            val seconds = ((elapsed / 1000).toInt()).coerceAtLeast(1)
            val node = nodes[assistantId] ?: return
            nodes[assistantId] = node.copy(
                reasoning = reasoning.toString(),
                text = content.toString(),
                reasoningSeconds = seconds,
                usage = usage,
            )
            _state.update { it.copy(messages = activePath()) }
        }

        try {
            repository.streamChat(
                baseUrl = baseUrl,
                apiKey = apiKey,
                model = model,
                messages = history,
                // 深度思考开关 → effort：开=max，关=low
                effort = effort,
            ).collect { event ->
                when (event) {
                    is StreamEvent.Reasoning -> {
                        if (reasoning.isEmpty()) {
                            nodes[assistantId]?.let {
                                nodes[assistantId] = it.copy(
                                    status = MessageStatus.Reasoning,
                                    reasoningExpanded = true,
                                )
                            }
                            _state.update { st ->
                                st.copy(generation = GenerationState.Reasoning, messages = activePath())
                            }
                        }
                        reasoning.append(event.delta)
                        flush()
                    }

                    is StreamEvent.Content -> {
                        if (firstContentAt == 0L) {
                            firstContentAt = System.currentTimeMillis()
                            nodes[assistantId]?.let {
                                nodes[assistantId] = it.copy(
                                    status = MessageStatus.Streaming,
                                    reasoningExpanded = false,
                                )
                            }
                            _state.update { st ->
                                st.copy(generation = GenerationState.Streaming, messages = activePath())
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
            nodes[assistantId]?.let {
                nodes[assistantId] = it.copy(status = MessageStatus.Completed, usage = usage)
            }
            _state.update {
                it.copy(
                    generation = GenerationState.Idle,
                    messages = activePath(),
                    sessionTokens = sessionTokens(),
                )
            }
            persist()
        } catch (e: kotlinx.coroutines.CancellationException) {
            flush(force = true)
            nodes[assistantId]?.let {
                nodes[assistantId] = it.copy(status = MessageStatus.Aborted)
            }
            _state.update {
                it.copy(generation = GenerationState.Idle, messages = activePath())
            }
            persist()
            throw e
        } catch (e: Exception) {
            flush(force = true)
            fail(assistantId, e.message ?: "请求失败，请稍后重试")
        }
    }

    private fun fail(assistantId: String, hint: String) {
        nodes[assistantId]?.let {
            nodes[assistantId] = it.copy(status = MessageStatus.Failed, errorHint = hint)
        }
        _state.update {
            it.copy(generation = GenerationState.Failed, messages = activePath())
        }
        persist()
    }

    /* ----------------------------- 树 / 持久化 ----------------------------- */

    /** 所有子节点（按创建顺序 = 分支顺序） */
    private fun childrenOf(parentId: String?): List<MessageUi> =
        nodes.values.filter { it.parentId == parentId }

    /** 从根到当前选中节点的可见消息；顺便把分支序号算好 */
    private fun activePath(): List<MessageUi> {
        val byParent = HashMap<String, MutableList<MessageUi>>()
        nodes.values.forEach { m -> byParent.getOrPut(m.parentId.orEmpty()) { mutableListOf() } += m }
        val path = mutableListOf<MessageUi>()
        val seen = HashSet<String>()
        var key = ""
        while (true) {
            val childId = activeChild[key] ?: break
            val node = nodes[childId] ?: break
            if (!seen.add(childId)) break
            path += node
            key = node.id
        }
        return path.map { node ->
            val siblings = byParent[node.parentId.orEmpty()].orEmpty()
            val index = siblings.indexOfFirst { it.id == node.id }
            node.copy(
                branchIndex = if (index >= 0) index + 1 else 1,
                branchCount = siblings.size.coerceAtLeast(1),
            )
        }
    }

    /** 从某节点回溯到根的完整路径（含自身），用于构造模型上下文 */
    private fun historyTo(nodeId: String): List<MessageUi> {
        val chain = mutableListOf<MessageUi>()
        var cursor: MessageUi? = nodes[nodeId]
        val seen = HashSet<String>()
        while (cursor != null && seen.add(cursor.id)) {
            chain += cursor
            cursor = cursor.parentId?.let { nodes[it] }
        }
        return chain.asReversed()
    }

    private fun sessionTokens(): Int = activePath().sumOf { it.usage?.total ?: 0 }

    private fun inferActiveChild(list: List<MessageUi>): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        list.forEach { map[it.parentId.orEmpty()] = it.id }
        return map
    }

    private fun persist() {
        if (nodes.isEmpty()) return
        store.upsert(
            StoredConversation(
                id = currentId,
                title = title.ifBlank { "新对话" },
                updatedAt = System.currentTimeMillis(),
                messages = nodes.values.toList(),
                activeChild = activeChild.toMap(),
                pinned = pinned,
                pinnedAt = pinnedAt,
                titleEdited = titleEdited,
            ),
        )
    }

    /** 发给模型的正文：带附件时把图片/文件信息（文本文件内容）拼在用户文字前面 */
    private fun promptFor(message: MessageUi): String {
        if (message.attachments.isEmpty()) return message.text
        val sb = StringBuilder()
        message.attachments.forEach { a ->
            if (a.isImage) {
                sb.append("[图片] ").append(a.displayName).append('\n')
            } else {
                sb.append("[附件] ").append(a.displayName).append('\n')
                readTextAttachment(a)?.let { body ->
                    sb.append("```\n").append(body).append("\n```\n")
                }
            }
        }
        if (message.text.isNotBlank()) sb.append('\n').append(message.text)
        return sb.toString().trim()
    }

    private fun readTextAttachment(a: AttachmentUi): String? {
        val path = a.path ?: return null
        if (!isTextFile(a.displayName)) return null
        return runCatching {
            val file = File(path)
            if (!file.length().let { it in 1..2_000_000 }) return null
            file.bufferedReader().use { reader ->
                val buffer = CharArray(20_000)
                val read = reader.read(buffer)
                if (read <= 0) null else String(buffer, 0, read)
            }
        }.getOrNull()
    }

    private fun isTextFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in TEXT_EXTENSIONS
    }

    private fun newId(prefix: String) = "$prefix-" + UUID.randomUUID().toString().take(8)

    val selectedModel: String get() = settings.model
    val hasApiKey: Boolean get() = settings.apiKey.isNotBlank()

    private companion object {
        val TEXT_EXTENSIONS = setOf(
            "txt", "md", "markdown", "csv", "json", "xml", "html", "htm", "log",
            "kt", "java", "py", "js", "ts", "tsx", "jsx", "css", "yml", "yaml", "ini", "conf", "sql",
        )
    }
}
