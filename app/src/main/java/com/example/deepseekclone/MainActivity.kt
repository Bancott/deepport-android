package com.example.deepseekclone

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepseekclone.data.AttachmentStore
import com.example.deepseekclone.data.SettingsStore
import com.example.deepseekclone.ui.camera.CameraScreen
import com.example.deepseekclone.ui.chat.ChatScreen
import com.example.deepseekclone.ui.chat.ChatViewModel
import com.example.deepseekclone.ui.chat.ConfirmDialog
import com.example.deepseekclone.ui.chat.ConversationDrawer
import com.example.deepseekclone.ui.chat.ConversationUi
import com.example.deepseekclone.ui.chat.DrawerUiState
import com.example.deepseekclone.ui.chat.PushDrawerLayout
import com.example.deepseekclone.ui.chat.RenameConversationDialog
import com.example.deepseekclone.ui.chat.SearchScreen
import com.example.deepseekclone.ui.chat.rememberVoiceInput
import com.example.deepseekclone.ui.web.WebScreen
import com.example.deepseekclone.ui.settings.AccountScreen
import com.example.deepseekclone.ui.settings.AppearanceOptions
import com.example.deepseekclone.ui.settings.FontSizeScreen
import com.example.deepseekclone.ui.settings.LanguageOptions
import com.example.deepseekclone.ui.settings.SingleChoiceDialog
import com.example.deepseekclone.ui.settings.DataScreen
import com.example.deepseekclone.ui.settings.ModelSettingsScreen
import com.example.deepseekclone.ui.settings.PersonalizationScreen
import com.example.deepseekclone.ui.settings.SettingsScreen
import com.example.deepseekclone.ui.settings.VoiceToneDialog
import com.example.deepseekclone.ui.share.ShareCard
import com.example.deepseekclone.ui.theme.DeepSeekTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
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
}

private enum class Screen { Chat, Search, Settings, Model, Account, Data, Personalization, FontSize }

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

    fun navPush(target: Screen) {
        if (navStack.last().screen == target) return
        navStack.add(NavEntry(target))
    }

    /** 返回一层；已经在栈底时返回 false，把返回键交还给系统 */
    fun navBack(): Boolean {
        if (navStack.size <= 1) return false
        navStack.removeAt(navStack.lastIndex)
        return true
    }

    /** 回到会话页并清掉中间层，用于点会话项 / 搜索结果 / 新建对话 */
    fun navResetToChat(drawerOpen: Boolean = false) {
        while (navStack.size > 1) navStack.removeAt(navStack.lastIndex)
        navStack[0] = NavEntry(Screen.Chat, drawerOpen)
    }

    /** 只改栈顶的抽屉状态；不能动更下层的记录，否则返回时还原不回来 */
    fun setDrawerOpen(open: Boolean) {
        navStack[navStack.lastIndex] = navStack.last().copy(drawerOpen = open)
    }
    var drawer by remember { mutableStateOf(DrawerUiState()) }
    var renameTarget by remember { mutableStateOf<ConversationUi?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationUi?>(null) }
    var showAppearance by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var webTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var showVoiceTone by remember { mutableStateOf(false) }

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

    // 预测性返回（Manifest 开 enableOnBackInvokedCallback 后系统送跟手进度）：
    // 手势期间页面按进度缩小+圆角预览，松手完成才走 pop，取消则弹回原状。
    // 返回优先级（同一个 Dispatcher 上后注册的先响应）：网页 → 上一页 → 关抽屉 → 系统退出。
    // 网页层必须单独兜住，否则它会跳过设置直接落在会话页，再按一次就退出了。
    var backProgress by remember { mutableFloatStateOf(0f) }
    var settleEpoch by remember { mutableStateOf(0) }

    // 手势结束（完成/取消）后把进度平滑归零：页面“松手回弹/收回”，不瞬跳
    LaunchedEffect(settleEpoch) {
        if (settleEpoch > 0 && backProgress > 0f) {
            animate(backProgress, 0f, tween(220)) { v, _ -> backProgress = v }
        }
    }

    PredictiveBackHandler(enabled = webTarget == null && navStack.size > 1) { progress ->
        try {
            progress.collect { backProgress = it.progress }
            navBack()
        } catch (e: CancellationException) {
            throw e
        } finally {
            settleEpoch++
        }
    }
    PredictiveBackHandler(enabled = webTarget == null && navStack.size == 1 && drawerOpen) { progress ->
        try {
            progress.collect { backProgress = it.progress }
            setDrawerOpen(false)
        } catch (e: CancellationException) {
            throw e
        } finally {
            settleEpoch++
        }
    }
    PredictiveBackHandler(enabled = webTarget != null) { progress ->
        try {
            progress.collect { backProgress = it.progress }
            webTarget = null
        } catch (e: CancellationException) {
            throw e
        } finally {
            settleEpoch++
        }
    }

    // 页面级预测性返回预览：手势期间整页跟手缩小（1→0.91）+ 圆角（0→22dp）。
    // 抽屉开着时不做（内容层已由 PushDrawerLayout 缩到 0.95，再叠会打架）。
    val previewModifier = Modifier.graphicsLayer {
        val p = if (navStack.size == 1 && drawerOpen) 0f else backProgress
        if (p > 0f) {
            val s = 1f - 0.09f * p
            scaleX = s
            scaleY = s
            shape = RoundedCornerShape(22.dp * p)
            clip = true
        }
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
                .background(MaterialTheme.colorScheme.background)
                .then(previewModifier),
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

    // 官方：页面切换由 Navigation-Compose 的默认转场完成——进入/返回都是
    // fadeIn/fadeOut(tween(700))，没有位移、缩放或共享元素（sz6.q / sz6.r → wa3 case 23/24）。
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(previewModifier),
    ) {
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            fadeIn(tween(700)) togetherWith fadeOut(tween(700))
        },
        label = "pageTransition",
    ) { target ->
    when (target) {
        Screen.Search -> SearchScreen(
            onBack = { navBack() },
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
            onBack = { navBack() },
        )

        Screen.Model -> ModelSettingsScreen(
            store = store,
            onBack = { navBack() },
        )

        Screen.Settings -> SettingsScreen(
            versionName = "0.1.0(1)",
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
            onAgreement = { webTarget = "https://cdn.deepseek.com/policies/zh-CN/deepseek-terms-of-use.html" to "服务协议" },
            onHelp = { webTarget = "https://chat.deepseek.com/" to "帮助与反馈" },
            onRecharge = { webTarget = "https://platform.deepseek.com/top_up" to "DeepSeek 官方充值" },
            onAppearance = { showAppearance = true },
        )

        Screen.Chat -> PushDrawerLayout(
            drawerOpen = drawerOpen,
            onDrawerOpenChange = { setDrawerOpen(it) },
            drawer = { modifier ->
                ConversationDrawer(
                    state = drawer,
                    modifier = modifier,
                    onSearchClick = {
                        // 保留抽屉的展开状态：搜索是从抽屉里进的，返回 pop 回来仍是
                        // "左历史右会话"，而不是掉到纯会话页
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
                        // 故意不关抽屉：会话页那层继续记着 open，从设置返回才能还原推页形态
                        navPush(Screen.Settings)
                    },
                )
            },
        ) {
            ChatScreen(
                state = chat,
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













