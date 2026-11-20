package io.github.bancott.deepport.ui.chat.web

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import io.github.bancott.deepport.ui.theme.DsDarkColors
import io.github.bancott.deepport.ui.theme.DsTheme
import org.json.JSONObject

/**
 * 用本地 WebView 渲染 AI 回答终稿：markdown-it + KaTeX + highlight.js。
 *
 * - 只用于「已完成且含富内容（公式/表格/代码/SVG）」的回答；流式与纯文本仍走原生。
 * - 高度由网页回传，WebView 自身不滚动，滚动交给外层 LazyColumn。
 * - 文本选择用 WebView 原生选择（系统手柄 + 系统工具栏）。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebMessageView(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    val density = LocalDensity.current
    var heightPx by remember { mutableIntStateOf(0) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val themeJs = remember(colors) {
        val fg = hex(colors.textPrimary.toArgb())
        val fg2 = hex(colors.textSecondary.toArgb())
        val outline = hex(colors.outline.toArgb())
        val link = hex(colors.primary.toArgb())
        val codeBg = hex(colors.surface2.toArgb())
        "window.setTheme({fg:'$fg',fg2:'$fg2',outline:'$outline',link:'$link',codeBg:'$codeBg',dark:${colors === DsDarkColors}})"
    }

    AndroidView(
        modifier = modifier
            .then(if (heightPx > 0) Modifier.height(with(density) { heightPx.toDp() }) else Modifier.wrapContentHeight()),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    textZoom = 100
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    loadWithOverviewMode = false
                    useWideViewPort = false
                }
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onHeight(h: Int) {
                            mainHandler.post {
                                if (h > 0 && h != heightPx) heightPx = h
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
                    }
                }
                loadUrl("file:///android_asset/web/message.html")
            }
        },
        update = { view ->
            // 内容或主题变化：只重渲染，不重建 WebView
            view.evaluateJavascript(themeJs, null)
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
