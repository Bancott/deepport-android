package com.example.deepseekclone.data

import com.example.deepseekclone.ui.chat.SourceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val role: String, val content: String)

/** SSE 解析后的事件 */
sealed interface StreamEvent {
    data class Reasoning(val delta: String) : StreamEvent
    data class Content(val delta: String) : StreamEvent
    data class Usage(val promptTokens: Int, val completionTokens: Int, val reasoningTokens: Int) : StreamEvent
    /** finish_reason，length 表示被输出额度截断 */
    data class Finish(val reason: String) : StreamEvent
    /** 接口回传的联网搜索结果（不同网关字段名不一样，统一归一化成 SourceItem） */
    data class Sources(val items: List<SourceItem>) : StreamEvent
    data object Done : StreamEvent
}

/**
 * 流式对话（SSE）。
 * 用 HttpURLConnection 读 `data:` 行，不引 OkHttp；`stream_options.include_usage`
 * 让最后一帧带上 usage，正好填到 UI 的 token 行。
 */
class ChatRepository {

    fun streamChat(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<ChatMessage>,
        // 深度思考会把思考过程也算进输出额度，1024 常常刚想完就没正文了，这里放到接口上限
        maxTokens: Int = 8192,
        effort: String? = null,
        enableSearch: Boolean = false,
    ): Flow<StreamEvent> = flow {
        val url = baseUrl.trimEnd('/') + "/chat/completions"
        val body = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("max_tokens", maxTokens)
            effort?.let { put("effort", it) }
            // 智能搜索：带上 search 标记，网关支持就真的联网（DeepSeek 官方接口不支持，见下面的 400 兜底）
            if (enableSearch) put("search", true)
            put("stream_options", JSONObject().put("include_usage", true))
            put(
                "messages",
                JSONArray().apply {
                    messages.forEach { m ->
                        put(JSONObject().put("role", m.role).put("content", m.content))
                    }
                },
            )
        }.toString()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 120_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "text/event-stream")
        }

        try {
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                // 有些供应商不认 search 字段会直接 400：静默退回不带搜索的请求，别让用户看到报错
                if (code == 400 && enableSearch) {
                    conn.disconnect()
                    emitAll(
                        streamChat(
                            baseUrl = baseUrl,
                            apiKey = apiKey,
                            model = model,
                            messages = messages,
                            maxTokens = maxTokens,
                            effort = effort,
                            enableSearch = false,
                        ),
                    )
                    return@flow
                }
                throw IllegalStateException(describeHttpError(code, err))
            }

            conn.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { raw ->
                    if (!raw.startsWith("data:")) return@forEach
                    val payload = raw.removePrefix("data:").trim()
                    if (payload.isEmpty()) return@forEach
                    if (payload == "[DONE]") {
                        emit(StreamEvent.Done)
                        return@useLines
                    }
                    val obj = runCatching { JSONObject(payload) }.getOrNull() ?: return@forEach

                    obj.optJSONArray("choices")?.let { choices ->
                        if (choices.length() > 0) {
                            val choice = choices.getJSONObject(0)
                            val delta = choice.optJSONObject("delta")
                            if (delta != null) {
                                delta.optText("reasoning_content")?.let { emit(StreamEvent.Reasoning(it)) }
                                delta.optText("content")?.let { emit(StreamEvent.Content(it)) }
                            }
                            choice.optText("finish_reason")?.let { emit(StreamEvent.Finish(it)) }
                        }
                    }

                    parseSources(obj)?.takeIf { it.isNotEmpty() }?.let { emit(StreamEvent.Sources(it)) }

                    obj.optJSONObject("usage")?.let { usage ->
                        val reasoning = usage
                            .optJSONObject("completion_tokens_details")
                            ?.optInt("reasoning_tokens", 0) ?: 0
                        emit(
                            StreamEvent.Usage(
                                promptTokens = usage.optInt("prompt_tokens", 0),
                                completionTokens = usage.optInt("completion_tokens", 0),
                                reasoningTokens = reasoning,
                            ),
                        )
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 语音转写（OpenAI 兼容的 /audio/transcriptions 表单上传）。
     * 供应商不统一，所以 baseUrl / key / model 都由设置里单独填。
     */
    fun transcribe(
        baseUrl: String,
        apiKey: String,
        model: String,
        audio: java.io.File,
    ): String {
        val url = baseUrl.trimEnd('/') + "/audio/transcriptions"
        val boundary = "----DSClone" + System.currentTimeMillis()
        val fileName = audio.name.ifBlank { "voice.m4a" }
        val head = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"model\"\r\n\r\n")
            append(model).append("\r\n")
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            append(fileName).append("\"\r\n")
            append("Content-Type: audio/mp4\r\n\r\n")
        }.toByteArray()
        val tail = "\r\n--$boundary--\r\n".toByteArray()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 90_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
            conn.setFixedLengthStreamingMode(head.size + audio.length() + tail.size)
            conn.outputStream.use { out ->
                out.write(head)
                audio.inputStream().use { it.copyTo(out) }
                out.write(tail)
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(describeHttpError(code, err))
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val text = runCatching { JSONObject(body).optString("text") }.getOrDefault("")
            return text.ifBlank { body.trim().removeSurrounding("\"") }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 从一帧里找搜索结果。常见字段：search_results / references / sources / web_search_results，
     * 每一项用 title|name、url|link、snippet|content|summary 的任意组合。
     */
    private fun parseSources(obj: JSONObject): List<SourceItem>? {
        val keys = listOf("search_results", "references", "sources", "web_search_results")
        for (key in keys) {
            val arr = obj.optJSONArray(key) ?: continue
            val items = mutableListOf<SourceItem>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val url = firstOf(o, "url", "link", "href")
                val title = firstOf(o, "title", "name", "site_name")
                if (url.isBlank() && title.isBlank()) continue
                items += SourceItem(
                    title = title.ifBlank { url },
                    url = url,
                    snippet = firstOf(o, "snippet", "content", "summary", "description"),
                )
            }
            if (items.isNotEmpty()) return items
        }
        return null
    }

    private fun firstOf(o: JSONObject, vararg keys: String): String {
        keys.forEach { key ->
            val value = o.optString(key)
            if (value.isNotBlank() && value != "null") return value
        }
        return ""
    }

    private fun describeHttpError(code: Int, body: String): String {        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        val base = when (code) {
            401 -> "API Key 无效（401），请到「设置 → 模型设置」重新填写"
            402 -> "余额不足（402）"
            403 -> "无访问权限（403）"
            404 -> "接口不存在（404），检查 Base URL"
            429 -> "请求过于频繁（429），稍后再试"
            in 500..599 -> "服务端错误（$code），稍后重试"
            else -> "请求失败（HTTP $code）"
        }
        return if (detail.isBlank()) base else "$base：$detail"
    }
}

/** JSON 里 content 可能是 null，optString 会返回字面量 "null"，这里统一过滤 */
private fun JSONObject.optText(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).takeIf { it.isNotEmpty() }
}

