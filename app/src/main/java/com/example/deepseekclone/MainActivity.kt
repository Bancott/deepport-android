package com.example.deepseekclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.deepseekclone.data.SettingsStore
import com.example.deepseekclone.ui.chat.ChatScreen
import com.example.deepseekclone.ui.chat.ChatViewModel
import com.example.deepseekclone.ui.chat.ConversationDrawer
import com.example.deepseekclone.ui.chat.ConversationGroup
import com.example.deepseekclone.ui.chat.ConversationUi
import com.example.deepseekclone.ui.chat.DrawerUiState
import com.example.deepseekclone.ui.chat.PushDrawerLayout
import com.example.deepseekclone.ui.settings.ModelSettingsScreen
import com.example.deepseekclone.ui.settings.SettingsScreen
import com.example.deepseekclone.ui.theme.DeepSeekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DeepSeekTheme {
                App()
            }
        }
    }
}

private enum class Screen { Chat, Settings, Model }

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember { SettingsStore.get(context) }
    val vm: ChatViewModel = viewModel()
    val chat by vm.state.collectAsStateWithLifecycle()

    var screen by remember { mutableStateOf(Screen.Chat) }
    var drawerOpen by remember { mutableStateOf(false) }
    var drawer by remember { mutableStateOf(DrawerUiState(groups = demoConversations())) }

    // 返回栈：模型设置 → 设置 → 会话；抽屉打开时先关抽屉。只有主界面才真正退出。
    BackHandler(enabled = screen != Screen.Chat) {
        screen = when (screen) {
            Screen.Model -> Screen.Settings
            Screen.Settings -> Screen.Chat
            else -> Screen.Chat
        }
    }
    BackHandler(enabled = screen == Screen.Chat && drawerOpen) {
        drawerOpen = false
    }

    when (screen) {
        Screen.Model -> ModelSettingsScreen(
            store = store,
            onBack = { screen = Screen.Settings },
        )

        Screen.Settings -> SettingsScreen(
            versionName = "0.1.0(1)",
            modelLabel = store.model.ifBlank { "未配置" },
            isSystemDark = isSystemInDarkTheme(),
            onBack = { screen = Screen.Chat },
            onModel = { screen = Screen.Model },
        )

        Screen.Chat -> PushDrawerLayout(
            drawerOpen = drawerOpen,
            onDrawerOpenChange = { drawerOpen = it },
            drawer = { modifier ->
                ConversationDrawer(
                    state = drawer,
                    modifier = modifier,
                    onSearchClick = { },
                    onConversationClick = { drawerOpen = false },
                    onLongPress = { conversation ->
                        drawer = drawer.copy(
                            groups = drawer.groups.map { group ->
                                group.copy(items = group.items.filterNot { it.id == conversation.id })
                            },
                        )
                    },
                    onPin = { conversation ->
                        drawer = drawer.copy(
                            groups = drawer.groups.map { group ->
                                group.copy(
                                    items = group.items.map {
                                        if (it.id == conversation.id) it.copy(pinned = !it.pinned) else it
                                    },
                                )
                            },
                        )
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
                        drawer = drawer.copy(multiSelect = false, selectedIds = emptySet())
                    },
                    onDeleteSelected = {
                        val ids = drawer.selectedIds
                        drawer = drawer.copy(
                            multiSelect = false,
                            selectedIds = emptySet(),
                            groups = drawer.groups.map { group ->
                                group.copy(items = group.items.filterNot { it.id in ids })
                            },
                        )
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
                onSend = { vm.send() },
                onStop = { vm.stop() },
                onToggleReasoning = { vm.toggleReasoning(it.id) },
                onRegenerate = { vm.regenerate(it) },
            )
        }
    }
}

/* ------------------------------- Demo 数据 ------------------------------- */

private fun demoConversations(): List<ConversationGroup> = listOf(
    ConversationGroup(
        label = "今天",
        items = listOf(
            ConversationUi("c1", "复刻 DeepSeek 安卓架构", "今天"),
            ConversationUi("c2", "其他模型 API 配置方法", "今天"),
            ConversationUi("c3", "Bancott 用户名评价", "今天"),
            ConversationUi("c4", "MFC 含义解析", "今天"),
            ConversationUi("c5", "二进制解码排序解释", "今天"),
            ConversationUi("c6", "王者英雄语音查看", "今天"),
        ),
    ),
    ConversationGroup(
        label = "昨天",
        items = listOf(
            ConversationUi("c7", "GitHub 新功能解析", "昨天"),
            ConversationUi("c8", "罗荣铸生日尺牍范文", "昨天"),
            ConversationUi("c9", "Bancott 含义解析", "昨天"),
            ConversationUi("c10", "Zotero 导出参考文献", "昨天"),
            ConversationUi("c11", "百度地图 BD09 坐标系", "昨天"),
            ConversationUi("c12", "香农熵解释", "昨天"),
            ConversationUi("c13", "组会汇报急救指南", "昨天"),
        ),
    ),
)

