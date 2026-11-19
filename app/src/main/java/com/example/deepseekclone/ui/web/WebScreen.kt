package com.example.deepseekclone.ui.web

import android.annotation.SuppressLint
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.SvgIcon
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 内置网页容器：服务协议、帮助与反馈、DeepSeek 官方充值都走这里。
 * 顶部固定返回键 + 标题，下方是 WebView（带加载进度条）。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(
    url: String,
    title: String,
    onBack: () -> Unit,
    onUserInfo: (name: String, avatar: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    var progress by remember { mutableStateOf(0) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    // 网页内部还能后退时，返回键优先走历史；历史空了才交给外层关掉页面
    var canGoBack by remember { mutableStateOf(false) }
    // 关闭时整页向下滑出后再移除：不做系统预测性返回那种跟手/缩放，
    // 就是很直接地"往下收走"。
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenHeightPx = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val offsetY = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            offsetY.animateTo(
                targetValue = screenHeightPx,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            )
            onBack()
        }
    }

    BackHandler(enabled = canGoBack) {
        webViewRef.value?.goBack()
    }
    // 历史空了：由这里接管返回，先播下滑动画再让宿主移除页面
    // （注册顺序比宿主晚，所以优先于宿主的 BackHandler）
    BackHandler(enabled = !canGoBack) {
        dismiss()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .graphicsLayer { translationY = offsetY.value }
            .statusBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            CircleIconButton(
                contentDescription = "返回",
                onClick = { dismiss() },
                containerColor = colors.surface2,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
            ) {
                SvgIcon(
                    R.drawable.ic_chevron_left_outline_20,
                    tint = colors.textPrimary,
                    size = 20.dp,
                )
            }
            Text(
                text = title,
                style = DsText.h2.copy(fontSize = 18.sp),
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = colors.primary,
                trackColor = colors.outline,
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    // 显式钉死 LTR：某些系统区域设置 / 输入法会让 WebView 把输入框按 RTL 处理，
                    // 表现就是手机号"从右往左"填、页面元素左右镜像。
                    layoutDirection = View.LAYOUT_DIRECTION_LTR
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        // 设置里的"字体大小"只作用于 App 自身，别让系统字体缩放把网页排版撑乱
                        textZoom = 100
                        // 按页面自带 viewport 自适应宽度，避免桌面版布局被横向裁切
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            progress = 100
                            canGoBack = view?.canGoBack() == true
                            // 页面是 React 渲染的，输入框拿到焦点后会被组件重新写成 RTL
                            // （dir="auto" 那套逻辑），所以三管齐下：
                            // 1) 注入带 !important 的样式表——优先级高于页面自己的内联 direction；
                            // 2) MutationObserver 监听 dir/style 属性变化，改了就打回去；
                            // 3) focusin 时再强制一次。
                            view?.evaluateJavascript(
                                "(function(){" +
                                    "var id='__ds_ltr_style';" +
                                    "if(!document.getElementById(id)){" +
                                    "var s=document.createElement('style');s.id=id;" +
                                    "s.textContent='input,textarea{direction:ltr !important;unicode-bidi:normal !important;text-align:left !important;}';" +
                                    "(document.head||document.documentElement).appendChild(s);}" +
                                    "document.documentElement.setAttribute('dir','ltr');" +
                                    "if(document.body){document.body.setAttribute('dir','ltr');}" +
                                    "var fix=function(){" +
                                    "document.querySelectorAll('input,textarea').forEach(function(e){" +
                                    "if(e.getAttribute('dir')!=='ltr'){e.setAttribute('dir','ltr');}" +
                                    "var p=e.parentElement;" +
                                    "while(p&&p!==document.documentElement){if(p.getAttribute('dir')!=='ltr'){p.setAttribute('dir','ltr');}p=p.parentElement;}" +
                                    "});};" +
                                    "fix();" +
                                    "if(!window.__dsCaretWatch){window.__dsCaretWatch=1;" +
                                    "/* 注意：整段脚本是一行，注释只能用块注释，行注释会把后面全部吞掉 */" +
                                    "/* 这个登录组件每次输入后把光标重置成 (0,0)，下一个字符就插到最前面，看起来像从右往左 */" +
                                    "document.addEventListener('beforeinput',function(ev){" +
                                    "var e=ev.target;if(!e||e.tagName!=='INPUT')return;" +
                                    "var t=ev.inputType;" +
                                    "if(t==='insertText'&&ev.data){" +
                                    "ev.preventDefault();" +
                                    "e.setRangeText(ev.data,e.value.length,e.value.length,'end');" +
                                    "e.dispatchEvent(new Event('input',{bubbles:true}));" +
                                    "}else if(t==='deleteContentBackward'){" +
                                    "ev.preventDefault();" +
                                    "var v=e.value||'';" +
                                    "if(v.length>0){e.setRangeText('',v.length-1,v.length,'end');" +
                                    "e.dispatchEvent(new Event('input',{bubbles:true}));}" +
                                    "}" +
                                    "},true);" +
                                    "new MutationObserver(fix).observe(document.documentElement," +
                                    "{childList:true,subtree:true,attributes:true,attributeFilter:['dir','style','class']});" +
                                    "document.addEventListener('focusin',function(ev){var t=ev.target;" +
                                    "if(t&&(t.tagName==='INPUT'||t.tagName==='TEXTAREA')){t.setAttribute('dir','ltr');}},true);" +
                                    "document.addEventListener('input',function(ev){var e=ev.target;" +
                                    "if(e&&(e.tagName==='INPUT'||e.tagName==='TEXTAREA')){" +
                                    "console.log('IP val=['+e.value+'] sel='+e.selectionStart+','+e.selectionEnd);}},true);}" +
                                    "})()",
                                null,
                            )
                            // 诊断：把页面真实的方向信息打到 logcat
                            view?.evaluateJavascript(
                                "(function(){var o={ua:navigator.userAgent,lang:navigator.language," +
                                    "htmlDir:document.documentElement.getAttribute('dir')," +
                                    "bodyDir:getComputedStyle(document.body).direction," +
                                    "inputs:[].map.call(document.querySelectorAll('input'),function(e){return e.type+'|'+getComputedStyle(e).direction+'|'+getComputedStyle(e).textAlign+'|'+getComputedStyle(e).unicodeBidi})};" +
                                    "return JSON.stringify(o)})()",
                            ) { r -> android.util.Log.i("WebProbe", "dir=$r") }
                            // 登录态回传：昵称和头像都在 <a href="/profile"> 里，
                            // 头像取 img.src，昵称取同一节点下的 span 文本（不是脱敏手机号）
                            view?.evaluateJavascript(
                                "(function(){setTimeout(function(){try{" +
                                    "var link=document.querySelector('a[href=\"/profile\"]');" +
                                    "var img=link?link.querySelector('img'):document.querySelector('.ds-avatar img');" +
                                    "var sp=link?link.querySelector('span'):null;" +
                                    "var name=sp?(sp.innerText||'').trim():'';" +
                                    "if(window.DSBridge&&(name||img)){DSBridge.post(JSON.stringify({" +
                                    "avatar:img?img.src:'',name:name}))}" +
                                    "}catch(e){}},1800)})()",
                                null,
                            )
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            canGoBack = view?.canGoBack() == true
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }

                        override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                            android.util.Log.i("WebConsole", "${msg.message()} @${msg.lineNumber()}")
                            return true
                        }
                    }
                    webViewRef.value = this
                    // 登录后把昵称/头像回传给原生：头像在 .ds-avatar img，
                    // 昵称在页面文本「当前帐户（182******20）」里
                    val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
                    addJavascriptInterface(
                        object {
                            @android.webkit.JavascriptInterface
                            fun post(json: String) {
                                runCatching {
                                    val o = org.json.JSONObject(json)
                                    val n = o.optString("name")
                                    val a = o.optString("avatar")
                                    if (n.isNotBlank() || a.isNotBlank()) {
                                        mainHandler.post { onUserInfo(n, a) }
                                    }
                                }
                            }
                        },
                        "DSBridge",
                    )
                    loadUrl(url)
                }
            },
            update = { view -> webViewRef.value = view },
        )
    }
}
