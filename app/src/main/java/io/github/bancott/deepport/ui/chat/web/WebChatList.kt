package io.github.bancott.deepport.ui.chat.web

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bancott.deepport.ui.chat.MessageStatus
import io.github.bancott.deepport.ui.chat.MessageUi
import io.github.bancott.deepport.ui.theme.DsDarkColors
import io.github.bancott.deepport.ui.theme.DsTheme
import org.json.JSONArray
import org.json.JSONObject

/**
 * 整个消息列表放进一个 WebView：网页是唯一滚动容器，原生只负责顶栏/输入栏。
 * 批次 A：静态渲染 + 滚动 + 主题；流式与动作桥在批次 B 接。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebChatList(
    messages: List<MessageUi>,
    sessionTokens: Int,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    var ready by remember { mutableStateOf(false) }
    val handler = remember { Handler(Looper.getMainLooper()) }

    val themeJs = remember(colors) {
        val fg = hex(colors.textPrimary.toArgb())
        val fg2 = hex(colors.textSecondary.toArgb())
        val outline = hex(colors.outline.toArgb())
        val link = hex(colors.primary.toArgb())
        val codeBg = hex(colors.surface2.toArgb())
        val bubble = hex(colors.bubble.toArgb())
        val surface2 = hex(colors.surface2.toArgb())
        "window.setTheme({fg:'$fg',fg2:'$fg2',outline:'$outline',link:'$link',codeBg:'$codeBg',bubble:'$bubble',surface2:'$surface2',dark:${colors === DsDarkColors}})"
    }

    val payload = remember(messages, sessionTokens) {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("role", if (m.role == io.github.bancott.deepport.ui.chat.ChatRole.User) "user" else "assistant")
                    put("text", m.text)
                    put("reasoning", m.reasoning)
                    put("reasoningSeconds", m.reasoningSeconds)
                    put("reasoningExpanded", m.reasoningExpanded)
                    put("status", m.status.name.lowercase())
                    put("sourcesCount", m.sourcesCount)
                    put("branchIndex", m.branchIndex)
                    put("branchCount", m.branchCount)
                    put("finishReason", m.finishReason ?: "")
                    put("errorHint", "")
                    m.usage?.let { u ->
                        put(
                            "usage",
                            JSONObject().put("prompt", u.prompt).put("completion", u.completion),
                        )
                    }
                },
            )
        }
        JSONObject().put("messages", arr).put("sessionTokens", sessionTokens).toString()
    }

    @Suppress("unused")
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                setBackgroundColor(colors.bg.toArgb())
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
                        fun onReady() {
                            handler.post { ready = true }
                        }

                        @JavascriptInterface
                        fun onContentChanged() {
                            // 批次 B：内容变化时在这里处理"回到底部"等
                        }
                    },
                    "AndroidHost",
                )
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                        Log.d("WebList", "console: ${msg?.message()} @${msg?.sourceId()}:${msg?.lineNumber()}")
                        return true
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view ?: return
                        view.evaluateJavascript(
                            "JSON.stringify([!!window.DP, !!window.setMessages, !!document.getElementById('list')])",
                        ) { r -> Log.d("WebList", "probe=$r") }
                        view.evaluateJavascript(themeJs, null)
                        view.evaluateJavascript("window.setMessages(${JSONObject.quote(payload)})") { r ->
                            Log.d("WebList", "setMessages result=$r")
                        }
                    }
                }
                loadUrl("file:///android_asset/web/list.html")
            }
        },
        update = { view ->
            if (!ready) return@AndroidView
            view.evaluateJavascript(themeJs, null)
            view.evaluateJavascript("window.setMessages(${JSONObject.quote(payload)})", null)
            view.evaluateJavascript("window.scrollToBottom()", null)
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
