package io.github.bancott.deepport.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 来源站点图标：接口在 web_search_result 里下发 `site_icon`（cdn.deepseek.com/site-icons/<domain>）。
 * 这里做内存 + 磁盘两级缓存，加载失败就回退到首字母徽标。
 */
object SiteIconStore {

    private const val MAX_ENTRIES = 64
    private const val MAX_BYTES = 256 * 1024

    private val memory = object : LruCache<String, Bitmap>(MAX_ENTRIES) {}

    fun cached(url: String): Bitmap? = if (url.isBlank()) null else memory.get(url)

    suspend fun load(context: Context, url: String): Bitmap? =
        withContext(Dispatchers.IO) {
            if (url.isBlank()) return@withContext null
            memory.get(url)?.let { return@withContext it }
            val file = cacheFile(context, url)
            if (file.exists()) {
                decode(file)?.let {
                    memory.put(url, it)
                    return@withContext it
                }
                file.delete()
            }
            val bitmap = download(url) ?: return@withContext null
            memory.put(url, bitmap)
            runCatching {
                file.parentFile?.mkdirs()
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            bitmap
        }

    private fun cacheFile(context: Context, url: String): File =
        File(File(context.cacheDir, "site_icons"), md5(url) + ".png")

    private fun decode(file: File): Bitmap? = runCatching {
        BitmapFactory.decodeFile(file.absolutePath)
    }.getOrNull()

    private fun download(url: String): Bitmap? = runCatching {
        var current = url
        repeat(3) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "DeepPort/1.0")
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                if (conn.contentLengthLong > MAX_BYTES) return@runCatching null
                val bytes = conn.inputStream.use { stream ->
                    val buffer = ByteArray(16 * 1024)
                    val out = java.io.ByteArrayOutputStream()
                    var total = 0
                    while (true) {
                        val read = stream.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_BYTES) return@runCatching null
                        out.write(buffer, 0, read)
                    }
                    out.toByteArray()
                }
                return@runCatching BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } finally {
                conn.disconnect()
            }
        }
        null
    }.getOrNull()

    private fun md5(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

/** 取缓存或异步加载站点图标；未加载完 / 失败时返回 null，由调用方回退首字母徽标 */
@Composable
fun rememberSiteIcon(url: String): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(url) { mutableStateOf(SiteIconStore.cached(url)) }
    LaunchedEffect(url) {
        if (url.isBlank() || bitmap != null) return@LaunchedEffect
        bitmap = SiteIconStore.load(context, url)
    }
    return bitmap
}
