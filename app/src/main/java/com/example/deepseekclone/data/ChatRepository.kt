package com.example.deepseekclone.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/** SSE 解析后的事件（UI 层只认这一套） */
sealed interface StreamEvent {
    data class Reasoning(val delta: String) : StreamEvent
    data class Content(val delta: String) : StreamEvent
    data class Usage(val promptTokens: Int, val completionTokens: Int, val reasoningTokens: Int) : StreamEvent
    /** finish_reason，length 表示被输出额度截断 */
    data class Finish(val reason: String) : StreamEvent
    /** 联网搜索来源 */
    data class Sources(val items: List<com.example.deepseekclone.ui.chat.SourceItem>) : StreamEvent
    /** 服务端补的会话标题（只请求一次） */
    data class Title(val title: String) : StreamEvent
    data object Done : StreamEvent
}

/**
 * 对话仓库：对接官方 chat-demo 的 Anthropic 兼容协议
 * （`{origin}/anthropic/v1/messages`，支持 thinking 开关、deepseek_web_search 工具、files API）。
 * 把协议事件翻译成 [StreamEvent]，UI/ViewModel 不感知协议差异。
 */
class ChatRepository(
    private val api: DeepSeekApi = DeepSeekApi(),
) {

    fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ApiMessage>,
        thinking: Boolean,
        enableSearch: Boolean,
        requestTitle: Boolean,
        system: String = DeepSeekApi.systemPrompt(),
        temperature: Float = 1.0f,
        thinkingBudget: Int? = null,
    ): Flow<StreamEvent> = api.streamMessage(
        origin = baseUrl,
        apiKey = apiKey,
        model = model,
        messages = messages,
        thinking = thinking,
        search = enableSearch,
        requestTitle = requestTitle,
        system = system,
        temperature = temperature,
        thinkingBudget = thinkingBudget,
    ).map { event ->
        when (event) {
            is ApiEvent.Thinking -> StreamEvent.Reasoning(event.delta)
            is ApiEvent.Text -> StreamEvent.Content(event.delta)
            is ApiEvent.Sources -> StreamEvent.Sources(event.items)
            is ApiEvent.Title -> StreamEvent.Title(event.title)
            is ApiEvent.Usage -> StreamEvent.Usage(
                promptTokens = event.input,
                completionTokens = event.output,
                reasoningTokens = 0,
            )
            is ApiEvent.Stop -> StreamEvent.Finish(
                if (event.reason == "max_tokens") "length" else event.reason,
            )
        }
    }

    /** 上传附件拿 file_id（files API），失败返回 null 由上层跳过该附件 */
    suspend fun upload(
        baseUrl: String,
        apiKey: String,
        file: File,
        name: String,
        isImage: Boolean,
    ): String? = withContext(Dispatchers.IO) {
        api.uploadFile(baseUrl, apiKey, file, DeepSeekApi.purposeFor(name, isImage))
    }

    suspend fun transcribe(baseUrl: String, apiKey: String, wav: File): String =
        withContext(Dispatchers.IO) { api.transcribe(baseUrl, apiKey, wav) }

    suspend fun synthesize(baseUrl: String, apiKey: String, text: String, voice: String): ByteArray =
        withContext(Dispatchers.IO) { api.synthesize(baseUrl, apiKey, text, voice) }

    /** 朗读用：边下边播的 PCM 流（24kHz / 单声道 / 16bit） */
    fun synthesizeStream(
        baseUrl: String,
        apiKey: String,
        text: String,
        voice: String,
    ): Flow<ByteArray> = api.synthesizeStream(baseUrl, apiKey, text, voice)
}
