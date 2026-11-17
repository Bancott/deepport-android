package com.example.deepseekclone.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.DeepSeekTopBar
import com.example.deepseekclone.ui.common.NewChatGlyph
import com.example.deepseekclone.ui.theme.DeepSeekTheme
import com.example.deepseekclone.ui.theme.DsDimen
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 会话页（第一阶段：空态 + 顶栏 + 输入卡）。
 * 消息流、流式态、分支条在后续迭代补齐，本文件保持单一职责：
 * 只负责骨架与状态分发，不含业务逻辑。
 */
@Composable
fun ChatScreen(
    state: ChatUiState,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {},
    onNewChatClick: () -> Unit = {},
    onToggleSpeechOutput: () -> Unit = {},
    onTextChange: (String) -> Unit = {},
    onToggleDeepThinking: () -> Unit = {},
    onToggleSmartSearch: () -> Unit = {},
    onAttachClick: () -> Unit = {},
    onToggleInputMode: () -> Unit = {},
    onDismissTranscribedNotice: () -> Unit = {},
    onSend: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    val colors = DsTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
    ) {
        DeepSeekTopBar(
            title = state.title,
            onMenuClick = onMenuClick,
        ) {
            CircleIconButton(
                contentDescription = if (state.isSpeechOutputOn) "关闭朗读" else "开启朗读",
                onClick = onToggleSpeechOutput,
            ) {
                Icon(
                    imageVector = if (state.isSpeechOutputOn) {
                        Icons.AutoMirrored.Outlined.VolumeUp
                    } else {
                        Icons.AutoMirrored.Outlined.VolumeOff
                    },
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(DsDimen.IconSize),
                )
            }
            CircleIconButton(
                contentDescription = "新建对话",
                onClick = onNewChatClick,
            ) {
                NewChatGlyph(color = colors.textPrimary)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            EmptyState(greeting = state.greeting)
        }

        DsInputBar(
            state = state.input,
            generating = state.isGenerating,
            onTextChange = onTextChange,
            onToggleDeepThinking = onToggleDeepThinking,
            onToggleSmartSearch = onToggleSmartSearch,
            onAttachClick = onAttachClick,
            onToggleMode = onToggleInputMode,
            onDismissTranscribedNotice = onDismissTranscribedNotice,
            onSend = onSend,
            onStop = onStop,
        )

        Text(
            text = "回答由 AI 生成，仅供参考",
            style = DsText.meta.copy(fontSize = 12.sp),
            color = colors.textTertiary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 3.dp, bottom = 10.dp),
        )
    }
}

@Composable
private fun EmptyState(greeting: String) {
    val colors = DsTheme.colors
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_deepseek_whale),
            contentDescription = "DeepSeek",
            tint = colors.primary,
            modifier = Modifier.size(44.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = greeting,
            style = DsText.display,
            color = colors.textPrimary,
        )
    }
}

@Preview(name = "浅色 · 空态", widthDp = 400, heightDp = 890, showBackground = true)
@Composable
private fun ChatEmptyLightPreview() {
    DeepSeekTheme(darkTheme = false) {
        ChatScreen(state = ChatUiState())
    }
}

@Preview(
    name = "深色 · 空态",
    widthDp = 400,
    heightDp = 890,
    showBackground = true,
    backgroundColor = 0xFF0F0F0F,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ChatEmptyDarkPreview() {
    DeepSeekTheme(darkTheme = true) {
        ChatScreen(state = ChatUiState(greeting = "你好，让我们开始聊天吧"))
    }
}

@Preview(
    name = "深色 · 输入中",
    widthDp = 400,
    heightDp = 890,
    showBackground = true,
    backgroundColor = 0xFF0F0F0F,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ChatTypingDarkPreview() {
    DeepSeekTheme(darkTheme = true) {
        ChatScreen(
            state = ChatUiState(
                input = InputUiState(
                    text = "如何做一个软件",
                    transcribedNotice = "🎙 已转为文字，可编辑",
                ),
            ),
        )
    }
}

@Preview(
    name = "浅色 · 语音模式",
    widthDp = 400,
    heightDp = 890,
    showBackground = true,
)
@Composable
private fun ChatVoiceLightPreview() {
    DeepSeekTheme(darkTheme = false) {
        ChatScreen(
            state = ChatUiState(
                input = InputUiState(mode = InputMode.Voice),
            ),
        )
    }
}



