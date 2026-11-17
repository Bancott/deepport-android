package com.example.deepseekclone.data

import com.example.deepseekclone.ui.chat.SourceItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 官方 chat-demo 同款协议（Anthropic 兼容）：
 * - 对话：POST {origin}/anthropic/v1/messages，`thinking` 开关 + `tools[deepseek_web_search]` 联网
 * - 文件：POST {origin}/anthropic/v1/files（multipart，需 `anthropic-beta: files-api-2025-04-14`）
 * - 语音：POST {origin}/v1/tools/asr、/v1/tools/tts
 * 文档来源：github.com/deepseek-ai/chat-demo
 */
data class ApiFile(val id: String, val isImage: Boolean)

/** 一条请求消息：text 为空时只发文件块 */
data class ApiMessage(
    val role: String,
    val text: String,
    val files: List<ApiFile> = emptyList(),
)

/** 流式事件 */
sealed interface ApiEvent {
    data class Thinking(val delta: String) : ApiEvent
    data class Text(val delta: String) : ApiEvent
    data class Sources(val items: List<SourceItem>) : ApiEvent
    data class Usage(val input: Int, val output: Int) : ApiEvent
    data class Stop(val reason: String) : ApiEvent
}

class DeepSeekApi {

    /* ------------------------------ 对话 ------------------------------ */

    fun streamMessage(
        origin: String,
        apiKey: String,
        model: String,
        messages: List<ApiMessage>,
        thinking: Boolean,
        search: Boolean,
    ): Flow<ApiEvent> = flow {
        val url = trimOrigin(origin) + "/anthropic/v1/messages"
        val body = JSONObject().apply {
            put("model", model)
            put("stream", true)
            put("thinking", JSONObject().put("type", if (thinking) "enabled" else "disabled"))
            put("safety_check", JSONObject().put("level", "high"))
            put("title", JSONObject().put("enabled", true))
            if (search) {
                put(
                    "tools",
                    JSONArray().put(
                        JSONObject()
                            .put("type", "deepseek_web_search")
                            .put("name", "deepseek_web_search"),
                    ),
                )
            }
            put(
                "messages",
                JSONArray().apply {
                    messages.forEach { m -> put(toMessageJson(m)) }
                },
            )
        }.toString()

        val hasFile = messages.any { it.files.isNotEmpty() }
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 180_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("Authorization", "Bearer $apiKey")
            if (hasFile) setRequestProperty("anthropic-beta", FILES_BETA)
        }

        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(describeError(code, err))
            }
            // 事件名与 JSON 里的 type 一致，直接按 data 里的 type 分发
            val sources = LinkedHashMap<String, SourceItem>()
            conn.inputStream.bufferedReader().useLines { lines ->
                var data = StringBuilder()
                lines.forEach { raw ->
                    when {
                        raw.startsWith("data:") -> data.append(raw.removePrefix("data:").trim())
                        raw.isBlank() -> {
                            if (data.isNotEmpty()) {
                                parseEvent(data.toString(), sources)?.let { emit(it) }
                                data = StringBuilder()
                            }
                        }
                    }
                }
            }
            if (sources.isNotEmpty()) emit(ApiEvent.Sources(sources.values.toList()))
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun toMessageJson(m: ApiMessage): JSONObject {
        if (m.files.isEmpty()) return JSONObject().put("role", m.role).put("content", m.text)
        val blocks = JSONArray()
        m.files.forEach { f ->
            blocks.put(
                JSONObject()
                    .put("type", if (f.isImage) "image" else "document")
                    .put("source", JSONObject().put("type", "file").put("file_id", f.id)),
            )
        }
        if (m.text.isNotBlank()) blocks.put(JSONObject().put("type", "text").put("text", m.text))
        return JSONObject().put("role", m.role).put("content", blocks)
    }

    private fun parseEvent(
        raw: String,
        sources: MutableMap<String, SourceItem>,
    ): ApiEvent? {
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        return when (obj.optString("type")) {
            "content_block_start" -> {
                val block = obj.optJSONObject("content_block") ?: return null
                if (block.optString("type") == "web_search_tool_result") {
                    val arr = block.optJSONArray("content") ?: return null
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        if (item.optString("type") != "web_search_result") continue
                        val url = item.optString("url")
                        if (url.isBlank()) continue
                        val title = item.optString("title").ifBlank { item.optString("site_name") }
                        sources[url] = SourceItem(
                            title = title.ifBlank { url },
                            url = url,
                            snippet = item.optString("snippet"),
                        )
                    }
                    null
                } else {
                    null
                }
            }

            "content_block_delta" -> {
                val delta = obj.optJSONObject("delta") ?: return null
                when (delta.optString("type")) {
                    "thinking_delta" -> ApiEvent.Thinking(delta.optString("thinking"))
                    "text_delta" -> ApiEvent.Text(delta.optString("text"))
                    "citations_delta" -> {
                        val c = delta.optJSONObject("citation") ?: return null
                        val url = c.optString("url")
                        if (url.isNotBlank()) {
                            sources[url] = SourceItem(
                                title = c.optString("title").ifBlank { url },
                                url = url,
                                snippet = c.optString("cited_text"),
                            )
                        }
                        null
                    }

                    else -> null
                }
            }

            "message_start" -> {
                val usage = obj.optJSONObject("message")?.optJSONObject("usage")
                usage?.let {
                    ApiEvent.Usage(
                        input = it.optInt("input_tokens"),
                        output = it.optInt("output_tokens"),
                    )
                }
            }

            "message_delta" -> {
                val usage = obj.optJSONObject("usage")
                if (usage != null) {
                    ApiEvent.Usage(
                        input = usage.optInt("input_tokens"),
                        output = usage.optInt("output_tokens"),
                    )
                } else {
                    val reason = obj.optJSONObject("delta")?.optString("stop_reason").orEmpty()
                    if (reason.isBlank()) null else ApiEvent.Stop(reason)
                }
            }

            "message_stop" -> null
            "error" -> {
                val msg = obj.optJSONObject("error")?.optString("message").orEmpty()
                throw IllegalStateException(msg.ifBlank { "生成中断" })
            }

            else -> null
        }
    }

    /* ------------------------------ 文件 ------------------------------ */

    /** 上传附件，返回 file_id；失败返回 null（发送时忽略该附件） */
    fun uploadFile(origin: String, apiKey: String, file: File, purpose: String?): String? {
        val url = trimOrigin(origin) + "/anthropic/v1/files"
        val boundary = "----DSClone" + System.currentTimeMillis()
        val fileName = file.name.ifBlank { "file" }
        val mime = guessMime(fileName)
        val head = buildString {
            if (purpose != null) {
                append("--").append(boundary).append("\r\n")
                append("Content-Disposition: form-data; name=\"purpose\"\r\n\r\n")
                append(purpose).append("\r\n")
            }
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"")
            append(fileName).append("\"\r\n")
            append("Content-Type: ").append(mime).append("\r\n\r\n")
        }.toByteArray()
        val tail = "\r\n--$boundary--\r\n".toByteArray()

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 120_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("anthropic-beta", FILES_BETA)
        }
        return try {
            conn.setFixedLengthStreamingMode(head.size + file.length() + tail.size)
            conn.outputStream.use { out ->
                out.write(head)
                file.inputStream().use { it.copyTo(out) }
                out.write(tail)
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                android.util.Log.w("DeepSeekApi", "upload failed $code: " + conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty())
                null
            } else {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(text).optString("id").takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            android.util.Log.w("DeepSeekApi", "upload error", e)
            null
        } finally {
            conn.disconnect()
        }
    }

    /* ------------------------------ 语音 ------------------------------ */

    /** 语音识别：WAV（单声道 16bit）或 Ogg Opus，≤120s / ≤8MiB */
    fun transcribe(origin: String, apiKey: String, wav: File): String {
        val url = trimOrigin(origin) + "/v1/tools/asr"
        val boundary = "----DSClone" + System.currentTimeMillis()
        val head = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"file\"; filename=\"recording.wav\"\r\n")
            append("Content-Type: audio/wav\r\n\r\n")
        }.toByteArray()
        val tail = "\r\n--$boundary--\r\n".toByteArray()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 120_000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
            conn.setFixedLengthStreamingMode(head.size + wav.length() + tail.size)
            conn.outputStream.use { out ->
                out.write(head)
                wav.inputStream().use { it.copyTo(out) }
                out.write(tail)
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(describeError(code, err))
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            return runCatching { JSONObject(text).optString("text") }.getOrDefault("")
        } finally {
            conn.disconnect()
        }
    }

    /** 语音合成：返回裸 PCM（24kHz / 单声道 / 16bit 小端） */
    fun synthesize(origin: String, apiKey: String, text: String, voice: String): ByteArray {
        val url = trimOrigin(origin) + "/v1/tools/tts"
        val body = JSONObject().apply {
            put("text", text)
            put("voice_id", voice)
            put("format", "pcm")
            put("filter_markdown", true)
        }.toString()
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 180_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray()) }
            val code = conn.responseCode
            if (code !in 200..299) {
                val err = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException(describeError(code, err))
            }
            return conn.inputStream.readBytes()
        } finally {
            conn.disconnect()
        }
    }

    /* ------------------------------ 工具 ------------------------------ */

    private fun describeError(code: Int, body: String): String {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        val base = when (code) {
            400 -> "请求参数有误（400）"
            401 -> "API Key 无效（401），请到「设置 → 模型设置」重新填写"
            402 -> "余额不足（402）"
            403 -> "无访问权限（403）"
            404 -> "接口不存在（404），检查 Base URL"
            413 -> "文件过大（413）"
            429 -> "请求过于频繁（429），稍后再试"
            in 500..599 -> "服务端错误（$code），稍后重试"
            else -> "请求失败（HTTP $code）"
        }
        return if (detail.isBlank()) base else "$base：$detail"
    }

    private fun guessMime(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "heic" -> "image/heic"
        "pdf" -> "application/pdf"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "ppt" -> "application/vnd.ms-powerpoint"
        "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        "txt", "md", "csv", "json" -> "text/plain"
        else -> "application/octet-stream"
    }

    private fun trimOrigin(origin: String): String {
        var s = origin.trim().trimEnd('/')
        // 允许用户把 /anthropic/v1 也填进 Base URL
        listOf("/anthropic/v1", "/v1").forEach { suffix ->
            if (s.endsWith(suffix)) s = s.removeSuffix(suffix)
        }
        return s
    }

    companion object {
        const val FILES_BETA = "files-api-2025-04-14"
        /** PDF / Word / PPT 需要服务端做排版识别，其余按纯数据上传 */
        fun purposeFor(name: String): String? = when (name.substringAfterLast('.', "").lowercase()) {
            "pdf", "doc", "docx", "ppt", "pptx" -> "rich_layout_recognition"
            else -> "user_data"
        }
    }
}
