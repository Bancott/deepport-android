package io.github.bancott.deepport.ui.chat.web

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bancott.deepport.ui.theme.DsDarkColors
import io.github.bancott.deepport.ui.theme.DsTheme
import org.json.JSONObject
import kotlin.math.max

/**
 * 回答正文统一用本地 WebView 渲染（chat-demo 同源渲染栈：micromark + llm-math + KaTeX + Prism）。
 *
 * - 流式与终稿是同一个 WebView，不做原生→网页切换，避免"闪一下"；
 * - 流式期间按 180ms 节流重渲染，终稿立即渲染一次；
 * - 高度由网页回传；测量完成前先按文本量估一个高度，避免从 0 撑开造成跳动；
 * - 代码块带语言标签 + 复制；文本选择用 WebView 原生选择。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebMessageView(
    markdown: String,
    modifier: Modifier = Modifier,
    streaming: Boolean = false,
) {
    val colors = DsTheme.colors
    val density = LocalDensity.current
    val context = LocalContext.current
    var heightPx by remember { mutableIntStateOf(0) }
    var lastRenderAt by remember { mutableLongStateOf(0L) }
    var lastRendered by remember { mutableStateOf<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bgArgb = colors.bg.toArgb()

    // 首次测量前的高度估计：按行数与每行字符数粗算，避免 0 高度闪一下
    val estimatedPx = remember(markdown, density) {
        val linePx = with(density) { 28.dp.toPx() }
        val lines = markdown.count { it == '\n' } + markdown.length / 22 + 1
        (max(2, lines) * linePx).toInt()
    }

    val themeJs = remember(colors) {
        val fg = hex(colors.textPrimary.toArgb())
        val fg2 = hex(colors.textSecondary.toArgb())
        val outline = hex(colors.outline.toArgb())
        val link = hex(colors.primary.toArgb())
        val codeBg = hex(colors.surface2.toArgb())
        "window.setTheme({fg:'$fg',fg2:'$fg2',outline:'$outline',link:'$link',codeBg:'$codeBg',dark:${colors === DsDarkColors}})"
    }

    AndroidView(
        modifier = modifier.height(with(density) { (if (heightPx > 0) heightPx else estimatedPx).toDp() }),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(bgArgb)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    textZoom = 100
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                }
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onHeight(h: Int) {
                            mainHandler.post {
                                if (h > 0) heightPx = h
                            }
                        }

                        @JavascriptInterface
                        fun copy(text: String) {
                            mainHandler.post {
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("code", text))
                            }
                        }
                    },
                    "AndroidHost",
                )
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view ?: return
                        view.evaluateJavascript(themeJs, null)
                        view.evaluateJavascript("window.render(${JSONObject.quote(markdown)})", null)
                        lastRendered = markdown
                    }
                }
                loadUrl("file:///android_asset/web/message.html")
            }
        },
        update = { view ->
            view.evaluateJavascript(themeJs, null)
            if (lastRendered == markdown) return@AndroidView
            val now = SystemClock.uptimeMillis()
            // 流式期间节流，终稿立即渲染
            if (streaming && now - lastRenderAt < 180L) return@AndroidView
            lastRenderAt = now
            lastRendered = markdown
            view.evaluateJavascript("window.render(${JSONObject.quote(markdown)})", null)
        },
        onRelease = { it.destroy() },
    )
}

private fun hex(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}
