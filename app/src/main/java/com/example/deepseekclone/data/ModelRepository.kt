package com.example.deepseekclone.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** 供应商 /models 返回的单个模型能力 */
data class ModelInfo(
    val id: String,
    val name: String,
    val contextWindow: Int = 0,
    val maxOutputTokens: Int = 0,
    val inputModalities: List<String> = emptyList(),
    val effortLevels: List<String> = emptyList(),
    val defaultEffort: String? = null,
) {
    val supportsImage: Boolean get() = inputModalities.contains("image")
}

/**
 * 从 `GET {baseUrl}/models` 拉取模型列表 —— 模型名一律以接口返回为准，不在本地写死。
 * 不引入 Retrofit/OkHttp，先用 HttpURLConnection 把链路跑通。
 */
class ModelRepository {

    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<ModelInfo>> =
        withContext(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalStateException("尚未填写 API Key"))
            }
            val url = baseUrl.trimEnd('/') + "/models"
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("Accept", "application/json")
                }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                conn.disconnect()

                if (code !in 200..299) {
                    return@withContext Result.failure(
                        IllegalStateException(describeHttpError(code, text)),
                    )
                }
                Result.success(parseModels(text))
            } catch (e: Exception) {
                Result.failure(IllegalStateException("网络错误：${e.javaClass.simpleName} ${e.message.orEmpty()}"))
            }
        }

    private fun parseModels(json: String): List<ModelInfo> {
        val root = JSONObject(json)
        val data = root.optJSONArray("data") ?: return emptyList()
        val result = mutableListOf<ModelInfo>()
        for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            val id = o.optString("id")
            if (id.isBlank()) continue
            val effort = o.optJSONObject("effort")
            result += ModelInfo(
                id = id,
                name = o.optString("name").ifBlank { id },
                contextWindow = o.optInt("context_window", 0),
                maxOutputTokens = o.optInt("max_output_tokens", 0),
                inputModalities = o.optJSONArray("input_modalities").toStringList(),
                effortLevels = effort?.optJSONArray("supported_levels").toStringList(),
                defaultEffort = effort?.optString("default_level")?.takeIf { it.isNotBlank() },
            )
        }
        return result
    }

    private fun describeHttpError(code: Int, body: String): String {
        val detail = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("")
        val base = when (code) {
            401 -> "API Key 无效（401）"
            402 -> "余额不足（402）"
            403 -> "无访问权限（403）"
            404 -> "接口不存在，检查 Base URL（404）"
            429 -> "请求过于频繁（429）"
            in 500..599 -> "服务端错误（$code）"
            else -> "请求失败（HTTP $code）"
        }
        return if (detail.isBlank()) base else "$base：$detail"
    }
}

private fun org.json.JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }
}

