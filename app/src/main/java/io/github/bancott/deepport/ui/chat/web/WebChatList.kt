package io.github.bancott.deepport.ui.chat.web

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.github.bancott.deepport.ui.chat.MessageUi
import io.github.bancott.deepport.ui.theme.DsDarkColors
import io.github.bancott.deepport.ui.theme.DsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.conflate
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.math.abs

/**
 * 整个消息列表放进一个 WebView：网页是唯一滚动容器，原生只负责顶栏/输入栏。
 *
 * 右滑开历史：位移由 [MessageWebView] 用 `MotionEvent.rawX` 直接算，再写进与顶栏共用的
 * [drawerProgress]。用 rawX 而不是网页里的 clientX，是因为会话页本身在被平移——
 * clientX 已经把这段平移扣掉了（实测手指走 750px、页面只走 433px，还带锯齿）。
 * 网页只负责上报两件只有 DOM 知道的事：落点是否在能横滑的代码块里、当前有没有选区。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebChatList(
    messages: List<MessageUi>,
    sessionTokens: Int,
    modifier: Modifier = Modifier,
    drawerProgress: MutableFloatState? = null,
    drawerTravelPx: Float = 1f,
    drawerOpen: Boolean = false,
    onSettle: (Boolean) -> Unit = {},
    onDragStart: () -> Unit = {},
    /** 动作条点击：copy / speak / share / regenerate / branch-prev / branch-next */
    onMessageAction: (MessageUi, String) -> Unit = { _, _ -> },
    /** 用户气泡长按：坐标为网页 CSS px（相对消息区左上角） */
    onUserMessageMenu: (MessageUi, Float, Float) -> Unit = { _, _, _ -> },
    /** 系统选择菜单里的「朗读」：从选区起点读到消息结尾 */
    onSpeakSelection: (String) -> Unit = {},
    /** 自增一次 = 清掉网页里的选区（朗读读完时用） */
    clearSelectionTick: Int = 0,
    /** 正在朗读的消息 id：网页据此给喇叭加波形动效 */
    readingMessageId: String? = null,
) {
    val colors = DsTheme.colors
    // 就绪状态由 WebView 自己持久记录（不是一次性的 onReady 事件）：
    // 进程被杀后重新打开时，页面往往在组合之前就加载完了，事件式的 ready 会永远收不到。
    val ready by ChatWeb.pageReady.collectAsState()
    val handler = remember { Handler(Looper.getMainLooper()) }
    val gesture = remember { GestureBridge() }
    var webView by remember { mutableStateOf<MessageWebView?>(null) }
    // 桥是页面加载时创建的，回调/lambda 必须每次重组更新到最新
    val messagesNow = rememberUpdatedState(messages)
    val actionNow = rememberUpdatedState(onMessageAction)
    val menuNow = rememberUpdatedState(onUserMessageMenu)
    val speakNow = rememberUpdatedState(onSpeakSelection)

    // 组合里的 lambda 每次重组都是新对象，桥里换成最新的一份
    SideEffect {
        gesture.progress = drawerProgress
        gesture.travelPx = drawerTravelPx.coerceAtLeast(1f)
        gesture.isOpen = drawerOpen
        gesture.onSettle = onSettle
        gesture.onDragStart = onDragStart
        gesture.onSpeakSelection = { text -> speakNow.value(text) }

        // 复用池里的 WebView 是单例，JS 接口只认 host，所以每次组合都把最新回调写进去
        val webHost = ChatWeb.host
        webHost.gesture = gesture
        webHost.messages = { messagesNow.value }
        webHost.onAction = { m, action -> actionNow.value(m, action) }
        webHost.onUserMenu = { m, x, y -> menuNow.value(m, x, y) }
        webHost.onReady = { ChatWeb.markReady() }
    }

    // 「设置 → 字体大小」在 Compose 里是往 LocalDensity.fontScale 里塞的，
    // 网页拿不到这个值，必须显式下发：整页字号都是 rem，改根字号就整页一起缩放。
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    val themeJs = remember(colors, fontScale) {
        val fg = hex(colors.textPrimary.toArgb())
        val fg2 = hex(colors.textSecondary.toArgb())
        val outline = hex(colors.outline.toArgb())
        val link = hex(colors.primary.toArgb())
        val codeBg = hex(colors.surface2.toArgb())
        val bubble = hex(colors.bubble.toArgb())
        val surface2 = hex(colors.surface2.toArgb())
        // 代码块配色取自官方 token：深色 block #1B1B1C / banner #2C2C2E，浅色 block #F9FAFB / banner 同色
        val dark = colors === DsDarkColors
        val codeBlock = if (dark) "#1B1B1C" else "#F2F3F5"
        val codeBanner = if (dark) "#2C2C2E" else "#F2F3F5"
        val inlineCode = if (dark) "#2C2C2E" else "#EBEEF2"
        // bg 也下发：来源胶囊的描边、站点图标之间的缝隙要用页面底色（官方 --dsw-alias-bg-base）
        val bg = hex(colors.bg.toArgb())
        "window.setTheme({fs:$fontScale,bg:'$bg',fg:'$fg',fg2:'$fg2',outline:'$outline',link:'$link',codeBg:'$codeBg',bubble:'$bubble',surface2:'$surface2',codeBlock:'$codeBlock',codeBanner:'$codeBanner',inlineCode:'$inlineCode',dark:$dark})"
    }

    val payload = remember(messages, sessionTokens, fontScale) {
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
                    // 回答末尾的「N 个网页」：标题 / 链接 / 站点图标，点开是原生 SourcesSheet
                    if (m.sources.isNotEmpty()) {
                        put(
                            "sources",
                            JSONArray().apply {
                                m.sources.forEach { item ->
                                    put(
                                        JSONObject().apply {
                                            put("title", item.title)
                                            put("url", item.url)
                                            put("siteIcon", item.siteIcon)
                                            put("snippet", item.snippet)
                                        },
                                    )
                                }
                            },
                        )
                    }
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
        JSONObject().put("messages", arr).put("sessionTokens", sessionTokens)
            .put("fs", fontScale.toDouble()).toString()
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            ChatWeb.obtain(ctx).also { view ->
                view.setBackgroundColor(colors.bg.toArgb())
                webView = view
            }
        },
        // 只在内容真的变化时下发：抽屉开合会触发整棵树重组，早先放在 update 里
        // 会导致松手那一帧整页 innerHTML 重刷 + 强制回底，看起来像页面自己跳了一下。
        update = { },
        // 只摘下来，不销毁：同一个 WebView 留给下一次进入会话复用（页面已加载好）
        onRelease = { },
    )

    LaunchedEffect(webView, ready, themeJs) {
        if (ready) webView?.evaluateJavascript(themeJs, null)
    }
    // 流式：按 66ms 节流下发，页面侧再按"只重画变化的那一条"增量更新。
    // conflate 会丢掉中间值、只保留最新的，保证最后一帧一定送达。
    val payloadNow = rememberUpdatedState(payload)
    LaunchedEffect(webView, ready) {
        if (!ready) return@LaunchedEffect
        snapshotFlow { payloadNow.value }
            .conflate()
            .collect { json ->
                webView?.evaluateJavascript("window.setMessages(${JSONObject.quote(json)})", null)
                delay(66)
            }
    }
    LaunchedEffect(webView, ready, readingMessageId) {
        if (ready) {
            webView?.evaluateJavascript(
                "window.setReadingMessage && window.setReadingMessage(${JSONObject.quote(readingMessageId ?: "")})",
                null,
            )
        }
    }
    // 回到前台兜底：WebView 可能被系统回收成空白页（尤其在"从后台划掉再打开"之后）。
    // 主题每次都重发（幂等）；消息只在页内条数对不上时才补发，避免把用户正在读的位置弹回底部。
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webView, ready) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val view = webView ?: return@LifecycleEventObserver
            if (!ready) return@LifecycleEventObserver
            view.evaluateJavascript(themeJs, null)
            view.evaluateJavascript("window.__needsMessages(${messages.size})") { raw ->
                if (raw == "true") {
                    view.evaluateJavascript("window.setMessages(${JSONObject.quote(payloadNow.value)})", null)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(webView, clearSelectionTick) {
        if (clearSelectionTick > 0) {
            webView?.evaluateJavascript("window.clearSelection && window.clearSelection()", null)
        }
    }
}

/** 手势与原生之间的共享状态；都由组合里的 SideEffect 更新到最新 */
internal class GestureBridge {
    @Volatile
    var progress: MutableFloatState? = null

    @Volatile
    var travelPx: Float = 1f

    @Volatile
    var isOpen: Boolean = false

    @Volatile
    var onSettle: (Boolean) -> Unit = {}

    @Volatile
    var onDragStart: () -> Unit = {}

    @Volatile
    var onSpeakSelection: (String) -> Unit = {}

    // 选区矩形（view 坐标，px）：Chromium 的 onGetContentRect 拿不到时当兜底
    @Volatile
    var selectionRect: android.graphics.RectF? = null

    @Volatile
    var actionMode: ActionMode? = null

    // 网页上报：落点所在的横滑容器还能不能朝某个方向滚 / 有没有选区
    @Volatile
    var codeBack: Boolean = false

    @Volatile
    var codeForward: Boolean = false

    @Volatile
    var selected: Boolean = false
}

/**
 * 消息区专用的 WebView：右滑开历史的手势在这里接管。
 *
 * 关键：用 [MotionEvent.getRawX]，它是**屏幕绝对坐标**，不会被 Compose 图层的平移影响，
 * 因此和顶栏 Compose 手势是同一个参考系——1:1 的换算可以照搬，手感才一致。
 *
 * 不接管的情况一律 `return super`：纵向滚动、点击、长按选择、以及"还能继续横滑"的代码块。
 * 注意：这里**不要**去覆写 canScrollHorizontally / 拦 scrollX——WebView 内部的代码块横滑
 * 和页面滚动都归 Chromium，抢过来只会让它滚不动。
 */
internal class MessageWebView(context: Context) : WebView(context) {

    /** 共享状态：复用池会整份换掉 bridge，所以每次用时都从 host 取最新的那一份 */
    private val bridge: GestureBridge get() = ChatWeb.host.gesture

    /** 页面是否已加载完：复用池据此决定要不要重新 loadUrl */
    var pageLoaded: Boolean = false

    private val slop = android.view.ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val samples = ArrayDeque<Pair<Float, Long>>()
    private var tracking = false
    private var dragging = false
    private var skipped = false
    private var downX = 0f
    private var downY = 0f
    private var startProgress = 0f

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                tracking = ev.pointerCount == 1
                dragging = false
                skipped = false
                downX = ev.rawX
                downY = ev.rawY
                samples.clear()
                samples.addLast(ev.rawX to ev.eventTime)
                return super.dispatchTouchEvent(ev)
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragging) {
                    samples.addLast(ev.rawX to ev.eventTime)
                    while (samples.size > 2 && ev.eventTime - samples.first().second > 60) {
                        samples.removeFirst()
                    }
                    applyDrag(ev.rawX - downX)
                    return true
                }
                if (!tracking || skipped || ev.pointerCount != 1) {
                    return super.dispatchTouchEvent(ev)
                }
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                val adx = abs(dx)
                val ady = abs(dy)
                // 判定点：累计位移到 1.5 倍 slop 再下结论，避免第一帧抖动就定性
                if (adx < slop * 1.5f && ady < slop * 1.5f) return super.dispatchTouchEvent(ev)
                // 纵向占优（夹角 < 50°）：这次手势就是纵向滚动，之后不再改判，
                // 否则手指划个弧线就会被半路抢走——表现就是"上下滑动很困难"
                if (ady > adx * 1.2f) {
                    tracking = false
                    return super.dispatchTouchEvent(ev)
                }
                // 只有明显偏水平（与竖直方向夹角 > 60°）才可能接管；斜向继续观察
                if (adx <= ady * 1.8f) return super.dispatchTouchEvent(ev)
                val opening = dx > 0f
                // 关着只接"开"，开着只接"关"
                if (opening == bridge.isOpen) {
                    skipped = true
                    return super.dispatchTouchEvent(ev)
                }
                // 有选区（拖手柄）或在能继续横滑的代码块/表格里：让给网页
                if (bridge.selected) {
                    skipped = true
                    return super.dispatchTouchEvent(ev)
                }
                if (if (opening) bridge.codeBack else bridge.codeForward) {
                    skipped = true
                    return super.dispatchTouchEvent(ev)
                }
                startDrag(ev, dx)
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    val velocity = releaseVelocity(ev.eventTime)
                    val progress = bridge.progress?.floatValue ?: 0f
                    val open = if (abs(velocity) >= FLING_VELOCITY_PX_PER_MS) {
                        velocity > 0f
                    } else {
                        progress >= 0.5f
                    }
                    dragging = false
                    tracking = false
                    bridge.onSettle(open)
                    return true
                }
                tracking = false
                return super.dispatchTouchEvent(ev)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    // ——— 系统选择菜单：加一个「朗读」 ———
    // 点它时不 finish 菜单：读的过程中选区保持可见，读完由原生调 window.clearSelection()。

    override fun startActionMode(callback: ActionMode.Callback?): ActionMode? =
        super.startActionMode(wrapSelectionMenu(callback))

    override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? =
        super.startActionMode(wrapSelectionMenu(callback), type)

    private fun wrapSelectionMenu(base: ActionMode.Callback?): ActionMode.Callback =
        object : ActionMode.Callback2() {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                bridge.actionMode = mode
                val ok = base?.onCreateActionMode(mode, menu) ?: true
                addSpeakItem(menu)
                return ok
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                val changed = base?.onPrepareActionMode(mode, menu) ?: false
                addSpeakItem(menu)
                return changed
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (item.itemId == SPEAK_ITEM_ID) {
                    speakSelectionTail()
                    return true
                }
                return base?.onActionItemClicked(mode, item) ?: false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                bridge.actionMode = null
                bridge.selectionRect = null
                base?.onDestroyActionMode(mode)
            }

            /**
             * 浮动菜单位置全靠这个回调：必须把选区矩形转发给 Chromium 原来的实现，
             * 否则 Callback2 的默认实现给的是空矩形——菜单会跑到屏幕最顶部。
             */
            override fun onGetContentRect(
                mode: ActionMode,
                view: View,
                outRect: android.graphics.Rect,
            ) {
                val origin = base as? ActionMode.Callback2
                if (origin != null) {
                    origin.onGetContentRect(mode, view, outRect)
                }
                if (outRect.isEmpty) {
                    // 拿不到 Chromium 的选区矩形时用网页上报的，避免浮条跑到屏幕顶部
                    val r = bridge.selectionRect
                    if (r != null) outRect.set(r.left.toInt(), r.top.toInt(), r.right.toInt(), r.bottom.toInt())
                }
            }

            private fun addSpeakItem(menu: Menu) {
                if (menu.findItem(SPEAK_ITEM_ID) == null) {
                    menu.add(Menu.NONE, SPEAK_ITEM_ID, Menu.NONE, "朗读")
                }
            }
        }

    /** 取「选区起点 → 本条消息结尾」的文字交给 App 内置 TTS */
    private fun speakSelectionTail() {
        evaluateJavascript("window.__selectionTail ? window.__selectionTail() : ''") { raw ->
            val text = (JSONTokener(raw).nextValue() as? String).orEmpty()
            if (text.isNotBlank()) bridge.onSpeakSelection(text)
        }
    }

    /** 接管第一帧：先打断 WebView 自己的手势流，再按「起点进度 + 位移」推进度 */
    private fun startDrag(ev: MotionEvent, dx: Float) {
        dragging = true
        bridge.onDragStart()
        startProgress = bridge.progress?.floatValue ?: 0f
        val cancel = MotionEvent.obtain(ev)
        cancel.action = MotionEvent.ACTION_CANCEL
        super.dispatchTouchEvent(cancel)
        cancel.recycle()
        applyDrag(dx)
    }

    private fun applyDrag(dx: Float) {
        // 扣掉起手的 slop，避免一接管就跳一下；位移是屏幕 px，行程也用 px
        val signed = if (dx > 0f) dx - slop else dx + slop
        val travel = bridge.travelPx.coerceAtLeast(1f)
        bridge.progress?.floatValue =
            (startProgress + signed / travel).coerceIn(0f, 1f)
    }

    private fun releaseVelocity(upTime: Long): Float {
        val first = samples.firstOrNull() ?: return 0f
        val last = samples.lastOrNull() ?: return 0f
        val dt = last.second - first.second
        // 松手前已经停住（>56ms 没有新采样）按静止处理，避免误判成甩动
        if (dt <= 0L || upTime - last.second > 56L) return 0f
        return (last.first - first.first) / dt
    }

    private companion object {
        /** 与 PushDrawerLayout 的 FLING_VELOCITY(700px/s) 对齐 */
        const val FLING_VELOCITY_PX_PER_MS = 0.7f

        /** 自定义菜单项 id（避开系统项） */
        const val SPEAK_ITEM_ID = 0x01020304
    }
}

private fun hex(argb: Int): String {
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return String.format("#%02X%02X%02X", r, g, b)
}

/** 网页 → 原生的可变回调集合：字段由组合在主线程写、JS 线程读，所以都加 @Volatile */
internal class ChatWebHost {
    @Volatile
    var gesture: GestureBridge = GestureBridge()

    @Volatile
    var messages: () -> List<MessageUi> = { emptyList() }

    @Volatile
    var onReady: () -> Unit = {}

    @Volatile
    var onAction: (MessageUi, String) -> Unit = { _, _ -> }

    @Volatile
    var onUserMenu: (MessageUi, Float, Float) -> Unit = { _, _, _ -> }
}

/**
 * 消息区 WebView 的预热 / 复用池。
 *
 * 以前每次进入会话都会 new 一个 WebView 并重新 loadUrl：Chromium 初始化 + list.html 解析
 * （katex / 字体 / 高亮）加起来 100~200ms，这成本正好落在"第一条消息出现"的那一刻，气泡会先空一下；
 * 会话切来切去还会反复付这笔钱。
 *
 * 现在整份 WebView 只建一次：应用启动后预热，空态 ↔ 会话、切换对话都复用同一个实例，页面不重新加载。
 * 回调不挂在实例上，而是挂在 [host] 上，每次进入会话由组合覆盖一遍，
 * 避免 WebView 长期持有已经销毁的 Compose 状态。
 */
internal object ChatWeb {
    private const val PAGE_URL = "file:///android_asset/web/list.html"

    val host = ChatWebHost()

    private val _pageReady = MutableStateFlow(false)

    /** 页面是否真正可下发内容。用 StateFlow 而不是一次性回调：进程重建时不会有"错过事件"的窗口 */
    val pageReady: StateFlow<Boolean> = _pageReady.asStateFlow()

    fun markReady() {
        _pageReady.value = true
    }

    private var instance: MessageWebView? = null

    fun obtain(context: Context): MessageWebView {
        val view = instance ?: create(context).also { instance = it }
        // 上一次的 AndroidView 可能还没摘干净，先确保没有父容器
        (view.parent as? ViewGroup)?.removeView(view)
        return view
    }

    fun destroy() {
        instance?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        instance = null
        resetReady()
    }

    fun resetReady() {
        _pageReady.value = false
    }

    private fun create(context: Context): MessageWebView = MessageWebView(context).apply {
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
        isNestedScrollingEnabled = false
        overScrollMode = View.OVER_SCROLL_NEVER
        // 按下后把事件锁在 WebView 里：纵向滚动/代码块横滑都交给 Chromium，
        // 父级 Compose 手势不要中途插手，否则慢速拖动会发涩。
        setOnTouchListener { v, e ->
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                v.parent?.requestDisallowInterceptTouchEvent(true)
            }
            false
        }
        addJavascriptInterface(HostApi(this), "AndroidHost")
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                Log.d("WebList", "console: ${msg?.message()} @${msg?.sourceId()}:${msg?.lineNumber()}")
                return true
            }
        }
        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                pageLoaded = false
                ChatWeb.resetReady()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                pageLoaded = true
                ChatWeb.markReady()
            }
        }
        loadUrl(PAGE_URL)
    }
}

/** 注入给网页的 `window.AndroidHost`：一辈子只注入一次，具体行为全从 [ChatWeb.host] 现取 */
private class HostApi(private val view: MessageWebView) {

    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onReady() {
        main.post { ChatWeb.host.onReady() }
    }

    @JavascriptInterface
    fun onContentChanged() {
        // 内容变化时预留的钩子（回到底部等由网页自己处理）
    }

    @JavascriptInterface
    fun setGestureContext(canScrollBack: Boolean, canScrollForward: Boolean) {
        val g = ChatWeb.host.gesture
        g.codeBack = canScrollBack
        g.codeForward = canScrollForward
    }

    @JavascriptInterface
    fun setSelectionState(selected: Boolean) {
        ChatWeb.host.gesture.selected = selected
    }

    @JavascriptInterface
    fun setSelectionRect(left: Double, top: Double, right: Double, bottom: Double) {
        val g = ChatWeb.host.gesture
        val d = view.resources.displayMetrics.density
        g.selectionRect = if (right > left || bottom > top) {
            android.graphics.RectF(
                (left * d).toFloat(),
                (top * d).toFloat(),
                (right * d).toFloat(),
                (bottom * d).toFloat(),
            )
        } else {
            null
        }
        main.post { g.actionMode?.invalidateContentRect() }
    }

    @JavascriptInterface
    fun messageAction(id: String, action: String) {
        val message = ChatWeb.host.messages().firstOrNull { it.id == id } ?: return
        main.post { ChatWeb.host.onAction(message, action) }
    }

    @JavascriptInterface
    fun userMessageMenu(id: String, x: Double, y: Double) {
        val message = ChatWeb.host.messages().firstOrNull { it.id == id } ?: return
        main.post { ChatWeb.host.onUserMenu(message, x.toFloat(), y.toFloat()) }
    }

    @JavascriptInterface
    fun copy(text: String) {
        main.post {
            val ctx = view.context
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("code", text))
            Toast.makeText(ctx, "已复制", Toast.LENGTH_SHORT).show()
        }
    }
}
