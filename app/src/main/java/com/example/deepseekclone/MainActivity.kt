package com.example.deepseekclone

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import android.app.Activity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
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
import com.example.deepseekclone.ui.chat.EditMessageDialog
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
import com.example.deepseekclone.ui.theme.DeepSeekTheme
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
                    App()
                }
            }
        }
    }
}

private enum class Screen { Chat, Search, Settings, Model, Account, Data, Personalization, FontSize }

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val vm: ChatViewModel = viewModel()
    val chat by vm.state.collectAsStateWithLifecycle()

    var screen by remember { mutableStateOf(Screen.Chat) }
    var drawerOpen by remember { mutableStateOf(false) }
    var drawer by remember { mutableStateOf(DrawerUiState()) }
    var renameTarget by remember { mutableStateOf<ConversationUi?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationUi?>(null) }
    var editTarget by remember { mutableStateOf<com.example.deepseekclone.ui.chat.MessageUi?>(null) }
    var showAppearance by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var webTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showCamera by remember { mutableStateOf(false) }

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

    // 重命名 / 置顶 / 删除之后都要从存储里重算分组，否则抽屉还显示旧标题
    fun refreshDrawer() {
        drawer = drawer.copy(groups = vm.conversationGroups())
    }

    // 返回栈：模型设置 → 设置 → 会话；抽屉打开时先关抽屉。只有主界面才真正退出。
    BackHandler(enabled = screen != Screen.Chat) {
        screen = when (screen) {
            Screen.Model, Screen.Account, Screen.Data, Screen.Personalization -> Screen.Settings
            Screen.Settings -> Screen.Chat
            else -> Screen.Chat
        }
    }
    BackHandler(enabled = screen == Screen.Chat && drawerOpen) {
        drawerOpen = false
    }

    if (showAppearance) {
        SingleChoiceDialog(
            title = "外观",
            options = AppearanceOptions,
            selected = store.themeMode,
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
            onSelect = { showLanguage = false },
            onDismiss = { showLanguage = false },
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

    editTarget?.let { target ->
        EditMessageDialog(
            initial = target.text,
            onConfirm = { newText -> vm.editUserMessage(target, newText) },
            onDismiss = { editTarget = null },
        )
    }

    // 打开抽屉时用真实会话列表刷新一次
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) refreshDrawer()
    }

    // WebView 页面（协议 / 帮助 / 官方充值）
    webTarget?.let { (url, title) ->
        WebScreen(
            url = url,
            title = title,
            onBack = { webTarget = null },
        )
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

    when (screen) {
        Screen.Search -> SearchScreen(
            onBack = { screen = Screen.Chat },
            onOpenConversation = { id ->
                vm.open(id)
                screen = Screen.Chat
            },
            search = { vm.search(it) },
        )

        Screen.FontSize -> FontSizeScreen(
            current = store.fontScale,
            onBack = { screen = Screen.Settings },
            onSelect = { store.updateFontScale(it) },
        )

        Screen.Account -> AccountScreen(onBack = { screen = Screen.Settings })

        Screen.Data -> DataScreen(onBack = { screen = Screen.Settings })

        Screen.Personalization -> PersonalizationScreen(onBack = { screen = Screen.Settings })

        Screen.Model -> ModelSettingsScreen(
            store = store,
            onBack = { screen = Screen.Settings },
        )

        Screen.Settings -> SettingsScreen(
            versionName = "0.1.0(1)",
            modelLabel = store.model.ifBlank { "未配置" },
            themeLabel = AppearanceOptions.firstOrNull { it.first == store.themeMode }?.second ?: "跟随系统",
            isSystemDark = isSystemInDarkTheme(),
            onBack = { screen = Screen.Chat },
            onModel = { screen = Screen.Model },
            onAccount = { screen = Screen.Account },
            onData = { screen = Screen.Data },
            onPersonalization = { screen = Screen.Personalization },
            onFontSize = { screen = Screen.FontSize },
            onLanguage = { showLanguage = true },
            onAgreement = { webTarget = "https://cdn.deepseek.com/policies/zh-CN/deepseek-terms-of-use.html" to "服务协议" },
            onHelp = { webTarget = "https://chat.deepseek.com/" to "帮助与反馈" },
            onRecharge = { webTarget = "https://platform.deepseek.com/top_up" to "DeepSeek 官方充值" },
            onAppearance = { showAppearance = true },
        )

        Screen.Chat -> PushDrawerLayout(
            drawerOpen = drawerOpen,
            onDrawerOpenChange = { drawerOpen = it },
            drawer = { modifier ->
                ConversationDrawer(
                    state = drawer,
                    modifier = modifier,
                    onSearchClick = {
                        drawerOpen = false
                        screen = Screen.Search
                    },
                    onConversationClick = { conversation ->
                        vm.open(conversation.id)
                        drawerOpen = false
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
                        drawerOpen = false
                        screen = Screen.Settings
                    },
                )
            },
        ) {
            ChatScreen(
                state = chat,
                onMenuClick = { drawerOpen = true },
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
                onEditMessage = { editTarget = it },
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
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message.text)
                    }
                    context.startActivity(Intent.createChooser(intent, "分享"))
                },
            )
        }
    }
}













