package com.example.deepseekclone.ui.chat

import androidx.compose.ui.draw.rotate

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.DeepSeekTopBar
import com.example.deepseekclone.ui.common.SvgIcon
import com.example.deepseekclone.ui.theme.DeepSeekTheme
import com.example.deepseekclone.ui.theme.DsDimen
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 会话页（第一阶段：空态 + 顶栏 + 输入卡）。
 * 消息流、流式态、分支条在后续迭代补齐，本文件保持单一职责：
 * 只负责骨架与状态分发，不含业务逻辑。
 */
@OptIn(ExperimentalLayoutApi::class)
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
    onToggleInputMode: () -> Unit = {},
    onDismissTranscribedNotice: () -> Unit = {},
    onRemoveAttachment: (AttachmentUi) -> Unit = {},
    onPickCamera: () -> Unit = {},
    onPickGallery: () -> Unit = {},
    onPickFile: () -> Unit = {},
    onSend: () -> Unit = {},
    onStop: () -> Unit = {},
    voiceRecording: Boolean = false,
    voiceCancel: Boolean = false,
    voiceLevels: List<Float> = emptyList(),
    onVoiceStart: () -> Unit = {},
    onVoiceCancelChange: (Boolean) -> Unit = {},
    onVoiceEnd: (Boolean) -> Unit = {},
    onToggleReasoning: (MessageUi) -> Unit = {},
    onCopyMessage: (MessageUi) -> Unit = {},
    onSpeakMessage: (MessageUi) -> Unit = {},
    onShareMessage: (MessageUi) -> Unit = {},
    onRegenerate: (MessageUi) -> Unit = {},
    onRetryMessage: (MessageUi) -> Unit = {},
    onBranchPrev: (MessageUi) -> Unit = {},
    onBranchNext: (MessageUi) -> Unit = {},
) {
    val colors = DsTheme.colors
    // 键盘弹起时：免责声明保持占位（避免收起时整页回跳），
    // 由输入栏自己把这 27dp 抵掉，保证栏底始终贴着键盘上沿 + 8dp。
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val disclaimerBlockPx = with(density) { DisclaimerBlockHeight.toPx() }
    val inputBottomPadding = with(density) {
        val extra = (imeBottomPx - disclaimerBlockPx).coerceAtLeast(0f)
        val gap = if (imeBottomPx > 0) 8.dp.toPx() else 0f
        (extra + gap).toDp()
    }
    // 附件面板：点「+」展开，键盘弹起 / 发出消息后自动收起
    var attachOpen by remember { mutableStateOf(false) }
    LaunchedEffect(imeBottomPx) { if (imeBottomPx > 0) attachOpen = false }
    LaunchedEffect(state.messages.size) { attachOpen = false }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    // 顶栏用用户提供的「声音」SVG；关闭朗读时叠一条斜线
                    Box(contentAlignment = Alignment.Center) {
                        SvgIcon(R.drawable.ic_action_volume, tint = colors.textPrimary, size = 22.dp)
                        if (!state.isSpeechOutputOn) {
                            Box(
                                Modifier
                                    .size(width = 26.dp, height = 1.8.dp)
                                    .rotate(-45f)
                                    .background(colors.textPrimary),
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                CircleIconButton(
                    contentDescription = "新建对话",
                    onClick = onNewChatClick,
                ) {
                    SvgIcon(R.drawable.ic_action_new_chat, tint = colors.textPrimary, size = 19.dp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (state.isEmpty) {
                    EmptyState(greeting = state.greeting)
                } else {
                    MessageStream(
                        messages = state.messages,
                        sessionTokens = state.sessionTokens,
                        onToggleReasoning = onToggleReasoning,
                        onCopy = onCopyMessage,
                        onSpeak = onSpeakMessage,
                        onShare = onShareMessage,
                        onRegenerate = onRegenerate,
                        onRetry = onRetryMessage,
                        onBranchPrev = onBranchPrev,
                        onBranchNext = onBranchNext,
                    )
                    BottomFadeMask(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        height = if (imeBottomPx > 0) 0 else 28,
                    )
                }
            }

            DsInputBar(
                modifier = Modifier
                    .padding(bottom = inputBottomPadding)
                    // 录音时保持挂载（手势不中断），视觉上让位给全屏录音样式
                    .alpha(if (voiceRecording) 0f else 1f),
                state = state.input,
                generating = state.isGenerating,
                onTextChange = onTextChange,
                onToggleDeepThinking = onToggleDeepThinking,
                onToggleSmartSearch = onToggleSmartSearch,
                onAttachClick = { attachOpen = !attachOpen },
                onRemoveAttachment = onRemoveAttachment,
                onToggleMode = onToggleInputMode,
                onDismissTranscribedNotice = onDismissTranscribedNotice,
                onVoiceStart = onVoiceStart,
                onVoiceCancelChange = onVoiceCancelChange,
                onVoiceEnd = onVoiceEnd,
                onSend = onSend,
                onStop = onStop,
            )

            AnimatedVisibility(
                visible = attachOpen && !voiceRecording,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 },
            ) {
                AttachmentPanel(
                    onCamera = {
                        attachOpen = false
                        onPickCamera()
                    },
                    onGallery = {
                        attachOpen = false
                        onPickGallery()
                    },
                    onFile = {
                        attachOpen = false
                        onPickFile()
                    },
                )
            }

            // 附件面板展开 / 录音中让位（官方这两块内容都不显示）
            if (!attachOpen && !voiceRecording) {
                Text(
                    text = "回答由 AI 生成，仅供参考",
                    style = DsText.meta.copy(fontSize = 12.sp),
                    color = colors.textTertiary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = DisclaimerBlockTopPadding, bottom = DisclaimerBlockBottomPadding),
                )
            }
        }

        // 按住说话：整屏泛光 + 提示 + 波形（盖在最上层；纯绘制，不拦触摸）
        if (voiceRecording) {
            VoiceOverlay(cancel = voiceCancel, levels = voiceLevels)
        }
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








/** 免责声明区块尺寸（文字行高 14 + 上 3 + 下 10），键盘弹起时由输入栏抵消 */
private val DisclaimerBlockTopPadding = 3.dp
private val DisclaimerBlockBottomPadding = 10.dp
private val DisclaimerBlockHeight = 27.dp








