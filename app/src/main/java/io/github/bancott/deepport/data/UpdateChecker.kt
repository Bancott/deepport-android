package io.github.bancott.deepport.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 检查 GitHub Release 是否有新版本。
 * 只读公开接口，不需要 token；失败（无网络 / 没有 Release）返回 failure，界面静默处理。
 */
object UpdateChecker {

    data class Release(
        val version: String,
        val url: String,
        val notes: String,
    )

    private const val API =
        "https://api.github.com/repos/Bancott/deepport-android/releases/latest"

    suspend fun latest(): Result<Release> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DeepPort-Android")
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            conn.disconnect()
            if (code !in 200..299) {
                return@withContext Result.failure(IllegalStateException("HTTP $code"))
            }
            val obj = JSONObject(text)
            val tag = obj.optString("tag_name").removePrefix("v")
            if (tag.isBlank()) return@withContext Result.failure(IllegalStateException("没有版本号"))
            Result.success(
                Release(
                    version = tag,
                    url = obj.optString("html_url"),
                    notes = obj.optString("body"),
                ),
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** a 是否比 b 新（按数字段比较，忽略前缀 v） */
    fun isNewer(a: String, b: String): Boolean {
        fun parts(v: String) = v.removePrefix("v")
            .split('.', '-')
            .mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        val x = parts(a)
        val y = parts(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val xi = x.getOrElse(i) { 0 }
            val yi = y.getOrElse(i) { 0 }
            if (xi != yi) return xi > yi
        }
        return false
    }
}
