package io.github.bancott.deepport

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.bancott.deepport.data.AttachmentStore
import io.github.bancott.deepport.data.SettingsStore
import io.github.bancott.deepport.ui.camera.CameraScreen
import io.github.bancott.deepport.BuildConfig
import io.github.bancott.deepport.ui.chat.ChatScreen
import io.github.bancott.deepport.ui.chat.ChatViewModel
import io.github.bancott.deepport.ui.chat.ConfirmDialog
import io.github.bancott.deepport.ui.chat.ConversationDrawer
import io.github.bancott.deepport.ui.chat.ConversationUi
import io.github.bancott.deepport.ui.chat.DrawerUiState
import io.github.bancott.deepport.ui.chat.PushDrawerLayout
import io.github.bancott.deepport.ui.chat.web.ChatWeb
import io.github.bancott.deepport.ui.chat.ReadAloudState
import io.github.bancott.deepport.ui.chat.RenameConversationDialog
import io.github.bancott.deepport.ui.chat.SearchScreen
import io.github.bancott.deepport.ui.chat.rememberVoiceInput
import io.github.bancott.deepport.ui.web.WebScreen
import io.github.bancott.deepport.ui.web.openInBrowserTab
import io.github.bancott.deepport.ui.settings.AboutScreen
import io.github.bancott.deepport.ui.settings.AccountScreen
import io.github.bancott.deepport.ui.settings.AppearanceOptions
import io.github.bancott.deepport.ui.settings.FontSizeScreen
import io.github.bancott.deepport.ui.settings.HelpScreen
import io.github.bancott.deepport.ui.settings.LanguageOptions
import io.github.bancott.deepport.ui.settings.SingleChoiceDialog
import io.github.bancott.deepport.ui.settings.DataScreen
import io.github.bancott.deepport.ui.settings.ModelSettingsScreen
import io.github.bancott.deepport.ui.settings.PersonalizationScreen
import io.github.bancott.deepport.ui.settings.SettingsScreen
import io.github.bancott.deepport.ui.settings.VoiceToneDialog
import io.github.bancott.deepport.ui.share.ShareCard
import io.github.bancott.deepport.ui.theme.DeepSeekTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // 注意：这里不做 WebView 预热。窗口就绪前创建的 WebView 在之后挂到 AndroidView 上时
        // 会一直不绘制（Chromium 认为它仍不可见），表现就是冷启动后消息区一片空白。
        // 复用池本身保留：第一次进入会话时创建，之后空态↔会话、切换对话都不再重建。
        setContent {
            val context = LocalContext.current
            val store = remember { SettingsStore.get(context) }
            val dark = when (store.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            // 主题切换时同步状态栏/导航栏图标明暗，否则浅色下仍是白图标、深色下仍是黑图标（看起来"乱"）
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as? Activity)?.window ?: return@SideEffect
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
            }

            DeepSeekTheme(darkTheme = dark) {
                val density = LocalDensity.current
                val scale = if (store.fontScale > 0f) store.fontScale else density.fontScale
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, scale),
                ) {
                    App(isDark = dark)
                }
            }
        }
    }

    override fun onDestroy() {
        // WebView 绑在当前 Activity 的 context 上，Activity 走了就必须销毁，不能跨实例复用
        ChatWeb.destroy()
        super.onDestroy()
    }
}

private enum class Screen { Chat, Search, Settings, Model, Account, Data, Personalization, FontSize, Help, About }

/**
 * 导航栈的一层。抽屉展开状态记在这一层上：
 * 从打开的抽屉进设置，会话页那层仍然是 open，返回时抽屉自然恢复，不会"跳一步"。
 */
private data class NavEntry(val screen: Screen, val drawerOpen: Boolean = false)

@Composable
private fun App(isDark: Boolean) {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val vm: ChatViewModel = viewModel()
    val chat by vm.state.collectAsStateWithLifecycle()

    // 导航栈：栈底永远是会话页，每进一层 push，返回就 pop 一层。
    // 抽屉状态挂在栈顶元素上，所以从抽屉进设置再返回时，抽屉会自动回到展开态。
    val navStack = remember { mutableStateListOf(NavEntry(Screen.Chat)) }
    val nav = navStack.last()
    val screen = nav.screen
    val drawerOpen = nav.drawerOpen
    // Custom Tabs 的顶栏配色（在组合里取好，回调里不能再读 MaterialTheme）
    val browserChromeColor = MaterialTheme.colorScheme.background.toArgb()
    // 转场方向：+1 = 进页（新页从右侧滑入），-1 = 返回（新页从左侧滑入）。
    // 官方 2.8.0 的录屏里返回就是「新页自左 1/12 屏宽滑入 + 淡入，旧页向右滑出 + 快速淡出」。
    var navForward by remember { mutableStateOf(true) }
    // 搜索是从抽屉里进的：进搜索时把历史界面收掉，返回时再恢复
    var searchFromDrawer by remember { mutableStateOf(false) }

    fun navPush(target: Screen) {
        if (navStack.last().screen == target) return
        navForward = true
        navStack.add(NavEntry(target))
    }

    /** 返回一层；已经在栈底时返回 false，把返回键交还给系统 */
    fun navBack(): Boolean {
        if (navStack.size <= 1) return false
        navForward = false
        navStack.removeAt(navStack.lastIndex)
        return true
    }

    /** 回到会话页并清掉中间层，用于点会话项 / 搜索结果 / 新建对话 */
    fun navResetToChat(drawerOpen: Boolean = false) {
        navForward = false
        while (navStack.size > 1) navStack.removeAt(navStack.lastIndex)
        navStack[0] = NavEntry(Screen.Chat, drawerOpen)
    }

    /** 只改栈顶的抽屉状态；不能动更下层的记录，否则返回时还原不回来 */
    fun setDrawerOpen(open: Boolean) {
        navStack[navStack.lastIndex] = navStack.last().copy(drawerOpen = open)
    }
    // 消息区（WebView）和顶栏（Compose 手势）共用同一份抽屉进度：
    // 顶栏用 dragAmount，消息区用 MotionEvent.rawX，两者都是"没有被图层平移污染"的坐标，
    // 所以 1:1 的换算完全一致，手感也就一致。
    val drawerProgress = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val drawerDensity = LocalDensity.current
    val drawerTravelPx = with(drawerDensity) {
        LocalConfiguration.current.screenWidthDp.dp.toPx() * 0.825f - 10.dp.toPx()
    }
    var drawerSettleSignal by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    var drawerSettleTarget by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var drawerCancelAnimSignal by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    /** 松手结算：目标态没变时，让 PushDrawerLayout 把进度收到锚点 */
    fun settleDrawer(open: Boolean) {
        if (drawerOpen != open) {
            setDrawerOpen(open)
        } else {
            drawerSettleTarget = if (open) 1f else 0f
            drawerSettleSignal++
        }
    }
    var drawer by remember { mutableStateOf(DrawerUiState()) }
    var renameTarget by remember { mutableStateOf<ConversationUi?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationUi?>(null) }
    var showAppearance by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var webTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var showVoiceTone by remember { mutableStateOf(false) }

    // 选区朗读：读的是「选区起点 → 消息结尾」，读完才退出选择（期间不打断）
    var selectionClearTick by remember { mutableIntStateOf(0) }
    var pendingSelectionClear by remember { mutableStateOf(false) }
    LaunchedEffect(chat.readingState) {
        if (pendingSelectionClear && chat.readingState == ReadAloudState.Idle) {
            pendingSelectionClear = false
            selectionClearTick++
        }
    }

    // 朗读失败（Key 未填 / 合成报错）用 toast 提示后清掉，避免重复弹
    LaunchedEffect(chat.speechError) {
        chat.speechError?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            vm.dismissSpeechError()
        }
    }

    val attachmentStore = remember { AttachmentStore(context) }
    val scope = rememberCoroutineScope()

    // 相册 / 文件都先把内容复制进私有目录，再作为待发送附件挂到输入卡
    fun importAttachment(uri: android.net.Uri) {
        scope.launch {
            val item = withContext(Dispatchers.IO) { attachmentStore.importUri(uri) }
            item?.let { vm.addAttachments(listOf(it)) }
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let { importAttachment(it) } }
    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { importAttachment(it) } }

    // 按住说话：录音 → 转写 → 回填输入框
    val voice = rememberVoiceInput(
        onRecorded = { file -> vm.transcribeAudio(file) },
        onDenied = { vm.failTranscription("需要麦克风权限才能使用语音输入") },
    )

    // 重命名 / 置顶 / 删除之后都要从存储里重算分组，否则抽屉还显示旧标题。
    // 必须切到 IO：conversationGroups() 里面是 runBlocking 的 Room 查询，
    // 直接在组合里调用会把主线程钉住，抽屉动画首帧必掉帧。
    fun refreshDrawer() {
        scope.launch {
            val groups = withContext(Dispatchers.IO) { vm.conversationGroups() }
            // 顺带把"当前会话"带上，抽屉里要给它蓝色高亮；还有登录后的昵称/头像
            drawer = drawer.copy(
                groups = groups,
                currentId = vm.currentConversationId(),
                userName = vm.userName(),
                avatarUrl = vm.userAvatar(),
            )
        }
    }

    // 启动时先备好列表，抽屉打开就能直接出内容，不用等一次查询
    LaunchedEffect(Unit) { refreshDrawer() }

    // 返回优先级（同一个 Dispatcher 上后注册的先响应）：网页 → 上一页 → 关抽屉 → 系统退出。
    // 网页层必须单独兜住，否则它会跳过设置直接落在会话页，再按一次就退出了。
    // 跟官方 2.8.0 录屏一致：拖拽期不做页面缩小预览（页面纹丝不动，只有系统返回把手），
    // 松手完成才走 pop 转场（下方 AnimatedContent 的 slide+fade）。
    BackHandler(enabled = webTarget == null && navStack.size > 1) {
        navBack()
    }
    BackHandler(enabled = webTarget == null && navStack.size == 1 && drawerOpen) {
        setDrawerOpen(false)
    }
    BackHandler(enabled = webTarget != null) {
        webTarget = null
    }

    if (showAppearance) {
        SingleChoiceDialog(
            title = "外观",
            options = AppearanceOptions,
            selected = store.themeMode,
            darkTheme = isDark,
            onSelect = {
                store.updateThemeMode(it)
                showAppearance = false
            },
            onDismiss = { showAppearance = false },
        )
    }
    if (showLanguage) {
        SingleChoiceDialog(
            title = "语言",
            options = LanguageOptions,
            selected = "zh-CN",
            darkTheme = isDark,
            onSelect = { showLanguage = false },
            onDismiss = { showLanguage = false },
        )
    }
    if (showVoiceTone) {
        VoiceToneDialog(
            selected = store.ttsVoice,
            onConfirm = { store.updateTtsVoice(it) },
            onDismiss = { showVoiceTone = false },
        )
    }

    renameTarget?.let { target ->
        RenameConversationDialog(
            initial = target.title,
            onConfirm = { newTitle ->
                vm.renameConversation(target.id, newTitle)
                refreshDrawer()
            },
            onDismiss = { renameTarget = null },
        )
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = "删除对话",
            message = "「${target.title}」删除后不可恢复。",
            onConfirm = {
                vm.deleteConversation(target.id)
                refreshDrawer()
            },
            onDismiss = { deleteTarget = null },
        )
    }

    // 打开抽屉时用真实会话列表刷新一次
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) refreshDrawer()
    }

    // WebView 页面（协议 / 帮助 / 官方充值）
    webTarget?.let { (url, title) ->
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            WebScreen(
                url = url,
                title = title,
                onBack = { webTarget = null },
                onUserInfo = { n, a -> vm.updateUserInfo(n, a) },
            )
        }
        return
    }

    // 应用内相机：拍完把临时文件导入附件，回到会话页
    if (showCamera) {
        CameraScreen(
            onClose = { showCamera = false },
            onCaptured = { file ->
                showCamera = false
                scope.launch {
                    val name = "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())}.jpg"
                    val item = withContext(Dispatchers.IO) { attachmentStore.importFile(file, name) }
                    item?.let { vm.addAttachments(listOf(it)) }
                }
            },
        )
        return
    }

    // 官方 2.8.0 返回转场（录屏 VID_20261119_185507 逐帧实测）：
    // 新页自左滑入 1/12 屏宽（≈33dp）+ fadeIn(200)，旧页向右滑出 1/12 + fadeOut(~100)；
    // 逐帧量到：新页位移 -76px→0（864px 宽 ≈ 1/11.4），约 230ms 落定，无缩放、无圆角。
    // 进页方向相反（新页自右滑入、旧页向左滑出），保持同一节奏。
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val enteringSearch = targetState == Screen.Search && initialState == Screen.Chat
            val leavingSearch = initialState == Screen.Search && targetState == Screen.Chat
            if (enteringSearch || leavingSearch) {
                // 搜索：搜索页从左侧被"推"进来，整个会话页被顶开往右让位；
                // 返回时反向。抽屉在进入搜索时已经收起，所以不会跟着走。
                val enter = if (enteringSearch) 1f else -1f
                (slideInHorizontally(tween(300)) { w -> (-enter * w / 5).toInt() } +
                    fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(300)) { w -> (enter * w / 4).toInt() } +
                        fadeOut(tween(220)))
            } else {
                val dir = if (navForward) 1 else -1
                (slideInHorizontally(tween(250)) { w -> dir * (w / 12) } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(250)) { w -> -dir * (w / 12) } + fadeOut(tween(100)))
            }
        },
        label = "pageTransition",
    ) { target ->
    when (target) {
        Screen.Search -> SearchScreen(
            onBack = {
                navBack()
                if (searchFromDrawer) {
                    setDrawerOpen(true)
                    searchFromDrawer = false
                }
            },
            onOpenConversation = { id ->
                vm.open(id)
                navResetToChat()
            },
            search = { vm.search(it) },
        )

        Screen.FontSize -> FontSizeScreen(
            current = store.fontScale,
            onBack = { navBack() },
            onSelect = { store.updateFontScale(it) },
        )

        Screen.Account -> AccountScreen(
            userName = store.userName,
            userAvatar = store.userAvatar,
            onBack = { navBack() },
        )

        Screen.Data -> {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            DataScreen(
                onBack = { navBack() },
                onShareCurrent = { vm.currentConversationText() },
                onExportAll = {
                    scope.launch {
                        val text = withContext(Dispatchers.IO) { vm.exportAllText() }
                        if (text.isBlank()) {
                            Toast.makeText(ctx, "还没有可导出的对话", Toast.LENGTH_SHORT).show()
                        } else {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "DeepSeek 对话导出")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            ctx.startActivity(Intent.createChooser(intent, "导出对话"))
                        }
                    }
                },
                onDeleteAll = {
                    scope.launch {
                        withContext(Dispatchers.IO) { vm.deleteAllConversations() }
                        refreshDrawer()
                        Toast.makeText(ctx, "已删除所有历史对话", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }

        Screen.Personalization -> PersonalizationScreen(
            autoCollapse = store.autoCollapseReasoning,
            onAutoCollapseChange = { store.updateAutoCollapseReasoning(it) },
            roleDefinition = store.roleDefinition,
            onRoleDefinitionChange = { store.updateRoleDefinition(it) },
            temperature = store.temperature,
            onTemperatureChange = { store.updateTemperature(it) },
            thinkingEffort = store.thinkingEffort,
            onThinkingEffortChange = { store.updateThinkingEffort(it) },
            darkTheme = isDark,
            onBack = { navBack() },
        )

        Screen.Help -> HelpScreen(onBack = { navBack() })

        Screen.About -> AboutScreen(
            versionName = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})",
            darkTheme = isDark,
            onBack = { navBack() },
            onOpenGitHub = {
                openInBrowserTab(
                    context = context,
                    url = "https://github.com/Bancott/deepport-android",
                    toolbarColor = browserChromeColor,
                )
            },
            onOpenHelp = { navPush(Screen.Help) },
            onOpenAgreement = {
                webTarget = "https://cdn.deepseek.com/policies/zh-CN/deepseek-terms-of-use.html" to "服务协议"
            },
            onCheckUpdate = {
                openInBrowserTab(
                    context = context,
                    url = "https://github.com/Bancott/deepport-android/releases",
                    toolbarColor = browserChromeColor,
                )
            },
        )

        Screen.Model -> ModelSettingsScreen(
            store = store,
            onBack = { navBack() },
        )

        Screen.Settings -> SettingsScreen(
            modelLabel = store.model.ifBlank { "未配置" },
            themeLabel = AppearanceOptions.firstOrNull { it.first == store.themeMode }?.second ?: "跟随系统",
            // 图标跟“最终生效主题”走，而不是系统深色；system 模式下两者相等，
            // 但用户强制浅色/深色时必须以 store.themeMode 的结果为准。
            isDarkTheme = isDark,
            voiceLabel = SettingsStore.TTS_VOICES.firstOrNull { it.first == store.ttsVoice }?.second ?: "海星",
            // 返回时 pop 回上一层，会话页那层记着"抽屉是开着的"，所以能还原成推页形态
            onBack = { navBack() },
            onModel = { navPush(Screen.Model) },
            onAccount = { navPush(Screen.Account) },
            onData = { navPush(Screen.Data) },
            onPersonalization = { navPush(Screen.Personalization) },
            onFontSize = { navPush(Screen.FontSize) },
            onLanguage = { showLanguage = true },
            onVoiceTone = { showVoiceTone = true },
            onAbout = { navPush(Screen.About) },
            // 充值要走「真浏览器」：支付 scheme 唤起、return_url 回跳、浏览器 Cookie 仓都只有
            // Custom Tabs / 系统浏览器具备，内置 WebView 里付不出去（详见 WebScreen 顶部说明）。
            onRecharge = {
                openInBrowserTab(
                    context = context,
                    url = "https://platform.deepseek.com/top_up",
                    toolbarColor = browserChromeColor,
                )
            },
            onAppearance = { showAppearance = true },
        )

        Screen.Chat -> PushDrawerLayout(
            drawerOpen = drawerOpen,
            onDrawerOpenChange = { setDrawerOpen(it) },
            externalProgress = drawerProgress,
            settleSignal = drawerSettleSignal,
            settleTarget = drawerSettleTarget,
            cancelAnimationSignal = drawerCancelAnimSignal,
            drawer = { modifier ->
                ConversationDrawer(
                    state = drawer,
                    modifier = modifier,
                    onSearchClick = {
                        // 进入搜索：历史界面先收起（整个右界面被顶开），返回时再恢复
                        searchFromDrawer = drawerOpen
                        if (drawerOpen) setDrawerOpen(false)
                        navPush(Screen.Search)
                    },
                    onConversationClick = { conversation ->
                        vm.open(conversation.id)
                        setDrawerOpen(false)
                    },
                    onLongPress = { conversation -> deleteTarget = conversation },
                    onRename = { conversation -> renameTarget = conversation },
                    onPin = { conversation ->
                        vm.togglePin(conversation.id)
                        refreshDrawer()
                    },
                    onToggleMultiSelect = { drawer = drawer.copy(multiSelect = true) },
                    onToggleSelect = { conversation ->
                        drawer = drawer.copy(
                            selectedIds = if (conversation.id in drawer.selectedIds) {
                                drawer.selectedIds - conversation.id
                            } else {
                                drawer.selectedIds + conversation.id
                            },
                        )
                    },
                    onExitMultiSelect = {
                        drawer = drawer.copy(multiSelect = false, selectedIds = emptySet())
                    },
                    onPinSelected = {
                        vm.setPinned(drawer.selectedIds, true)
                        drawer = drawer.copy(multiSelect = false, selectedIds = emptySet())
                        refreshDrawer()
                    },
                    onDeleteSelected = {
                        vm.deleteConversations(drawer.selectedIds)
                        drawer = drawer.copy(multiSelect = false, selectedIds = emptySet())
                        refreshDrawer()
                    },
                    onFooterClick = {
                        // 官方 2.8.0：从抽屉进设置时就收起抽屉，返回直接落在会话页
                        // （不是「左历史右会话」的推页态）。
                        setDrawerOpen(false)
                        navPush(Screen.Settings)
                    },
                )
            },
        ) {
            ChatScreen(
                state = chat,
                drawerOpen = drawerOpen,
                // 官方顶栏左键是「开/关侧边栏」：抽屉开着时同一按钮点一下收起
                onMenuClick = { setDrawerOpen(!drawerOpen) },
                menuDescription = if (drawerOpen) "关闭会话列表" else "打开会话列表",
                onNewChatClick = { vm.newChat() },
                onToggleSpeechOutput = { vm.toggleSpeechOutput() },
                onTextChange = { vm.updateInput(it) },
                onToggleDeepThinking = { vm.toggleDeepThinking() },
                onToggleSmartSearch = { vm.toggleSmartSearch() },
                onToggleInputMode = { vm.toggleInputMode() },
                onDismissTranscribedNotice = { vm.dismissTranscribedNotice() },
                onRemoveAttachment = { vm.removeAttachment(it.id) },
                onPickCamera = { showCamera = true },
                onPickGallery = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onPickFile = { pickFile.launch(arrayOf("*/*")) },
                voiceRecording = voice.recording,
                voiceCancel = voice.cancelHint,
                voiceLevels = voice.levels,
                onVoiceStart = { voice.start() },
                onVoiceCancelChange = { voice.updateCancel(it) },
                onVoiceEnd = { cancel -> voice.finish(cancel) },
                onSend = { vm.send() },
                onStop = { vm.stop() },
                onToggleReasoning = { vm.toggleReasoning(it.id) },
                onRegenerate = { vm.regenerate(it) },
                onRetryMessage = { vm.retry(it) },
                onBranchPrev = { vm.selectBranch(it, -1) },
                onBranchNext = { vm.selectBranch(it, 1) },
                onEditMessage = { vm.beginEdit(it) },
                onCancelEdit = { vm.cancelEdit() },
                onOpenSource = { url -> webTarget = url to "来源" },
                onCopyMessage = { message ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("message", message.text))
                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                },
                onSpeakSelection = { text, messageId ->
                    if (vm.speakSelection(text, messageId)) pendingSelectionClear = true
                },
                onStopSpeaking = { vm.stopSpeaking() },
                onContinueMessage = { vm.continueMessage(it) },
                clearSelectionTick = selectionClearTick,
                drawerProgress = drawerProgress,
                drawerTravelPx = drawerTravelPx,
                onDrawerSettle = { settleDrawer(it) },
                onDrawerDragStart = { drawerCancelAnimSignal++ },
                onSpeakMessage = { message ->
                    if (!vm.speakMessage(message)) {
                        Toast.makeText(context, "这条回答还没有可朗读的正文", Toast.LENGTH_SHORT).show()
                    }
                },
                onShareMessage = { message ->
                    val file = ShareCard.render(context, chat.title, message.text)
                    val uri = file?.let {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            it,
                        )
                    }
                    if (uri != null) {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "分享"))
                    } else {
                        Toast.makeText(context, "这条回答还没有可分享的正文", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }
    }
    }
}













