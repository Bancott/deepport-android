package com.example.deepseekclone.ui.chat

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.DeepSeekTopBar
import com.example.deepseekclone.ui.common.NewChatGlyph
import com.example.deepseekclone.ui.common.SpeakerGlyph
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
    menuDescription: String = "打开会话列表",
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
    onEditMessage: (MessageUi) -> Unit = {},
    onOpenSource: (String) -> Unit = {},
    onBranchPrev: (MessageUi) -> Unit = {},
    onBranchNext: (MessageUi) -> Unit = {},
    onCancelEdit: () -> Unit = {},
) {
    val colors = DsTheme.colors
    val focusManager = LocalFocusManager.current
    // 键盘弹起时：免责声明保持占位（避免收起时整页回跳），
    // 由输入栏自己把这 27dp 抵掉，保证栏底始终贴着键盘上沿 + 8dp。
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val disclaimerBlockPx = with(density) { DisclaimerBlockHeight.toPx() }

    // 附件面板：点「+」展开，键盘弹起 / 发出消息后自动收起
    var attachOpen by remember { mutableStateOf(false) }
    var sourcesSheet by remember { mutableStateOf<List<SourceItem>?>(null) }
    // 「选择文字」面板：菜单点「选择文本」后把整条消息文本交给模态面板
    var selectionText by remember { mutableStateOf<String?>(null) }
    // 正在原地选中的那条消息；点空白处即取消
    var messageSelection by remember { mutableStateOf<MessageSelection?>(null) }
    var selectionStartedAt by remember { mutableLongStateOf(0L) }
    // 只在键盘"弹起"时收面板。原来写的是 imeBottomPx > 0，键盘收起途中每帧都 > 0，
    // 会把刚点加号打开的面板立刻关掉（表现为"点加号没反应"）。
    var lastImePx by remember { mutableIntStateOf(0) }
    // 持续记录"最近一次非零的键盘高度"。键盘收起时 ime 会归零，
    // 但插值仍需要它当起点；不能在点加号那一刻去读——那时 clearFocus 已生效，很可能读到 0。
    var lastNonZeroIme by remember { mutableIntStateOf(0) }
    LaunchedEffect(imeBottomPx) {
        if (imeBottomPx > lastImePx) attachOpen = false
        if (imeBottomPx > 0) lastNonZeroIme = imeBottomPx
        lastImePx = imeBottomPx
    }
    LaunchedEffect(state.messages.size) { attachOpen = false }

    // 面板进度同时驱动两件事：输入栏让出的键盘留白、面板自身高度。
    // 用一个值驱动，两者之和只会单调变化——不会再出现"先被面板顶上去、再被键盘留白拽下来"的折返。
    val panelVisible = attachOpen && !voiceRecording
    // 官方面板尺寸变化走 150ms、ease-out（cubic-bezier(0,0,0.58,1)）
    val panelEasing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
    val panelProgress by animateFloatAsState(
        targetValue = if (panelVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 150, easing = panelEasing),
        label = "panelProgress",
    )
    // 展开/收起期间都用锁存的键盘高度插值：如果收起时立刻切回 imeBottomPx（通常已经是 0），
    // 输入栏会在面板退场的第一帧整体下坠，表现就是点“文件”后闪一下。
    val panelAnimating = attachOpen || panelProgress > 0.001f
    val padSourcePx = if (panelAnimating) maxOf(lastNonZeroIme, imeBottomPx) else imeBottomPx
    val inputBottomPadding = with(density) {
        val gapPx = 8.dp.toPx()
        val extra = (padSourcePx - disclaimerBlockPx).coerceAtLeast(0f)
        // 间隙连续衰减：原来写成 if (imeBottomPx > 0) 8.dp else 0.dp，
        // 键盘收到最后一帧时它会从 8dp 直接归零，输入栏会"跳一下"。
        val gap = gapPx * (extra / gapPx).coerceIn(0f, 1f)
        ((extra + gap) * (1f - panelProgress)).toDp()
    }
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
                menuDescription = menuDescription,
            ) {
                // 顶栏朗读按钮：官方三态图标，不再手绘斜线/声波。
                val reading = state.readingState != ReadAloudState.Idle
                val speechOn = state.isSpeechOutputOn
                CircleIconButton(
                    contentDescription = if (speechOn) "关闭自动朗读" else "开启自动朗读",
                    onClick = onToggleSpeechOutput,
                ) {
                    SpeakerGlyph(
                        color = if (reading) colors.primary else colors.textPrimary,
                        muted = !speechOn,
                        reading = reading,
                        iconSize = 24.dp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                CircleIconButton(
                    contentDescription = "新建对话",
                    onClick = onNewChatClick,
                ) {
                    NewChatGlyph(colors.textPrimary, iconSize = 24.dp)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // 点正文空白处：收起附件面板 + 收键盘。
                    // 复制 / 分享 / 来源这些按钮自己会消费 tap，不会冒泡到这里，所以不受影响。
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            attachOpen = false
                            // 菜单项那一下点击可能紧跟着穿透到这里，300ms 内不误清刚建立的选区
                            if (System.currentTimeMillis() - selectionStartedAt > 300) {
                                messageSelection = null
                            }
                        })
                    },
            ) {
                if (state.isEmpty) {
                    EmptyState(greeting = state.greeting)
                } else {
                    MessageStream(
                        messages = state.messages,
                        sessionTokens = state.sessionTokens,
                        selection = messageSelection,
                        onSelectionChange = { sel ->
                            messageSelection = sel
                            if (sel != null) selectionStartedAt = System.currentTimeMillis()
                        },
                        readingMessageId = state.readingMessageId,
                        readingState = state.readingState,
                        onToggleReasoning = onToggleReasoning,
                        onCopy = onCopyMessage,
                        onSpeak = onSpeakMessage,
                        onShare = onShareMessage,
                        onRegenerate = onRegenerate,
                        onRetry = onRetryMessage,
                        onEdit = onEditMessage,
                        onSourcesClick = { message -> sourcesSheet = message.sources },
                        onBranchPrev = onBranchPrev,
                        onBranchNext = onBranchNext,
                        editingId = state.editing?.id,
                        onCancelEdit = onCancelEdit,
                        onSelectTextSheet = { message -> selectionText = message.text },
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
                attachOpen = attachOpen,
                editing = state.editing != null,
                onCancelEdit = onCancelEdit,
                onTextChange = onTextChange,
                onToggleDeepThinking = onToggleDeepThinking,
                onToggleSmartSearch = onToggleSmartSearch,
                onAttachClick = {
                    if (attachOpen) {
                        attachOpen = false
                    } else {
                        // 先收键盘再开面板：键盘还在时开面板会被 ime 变化逻辑立刻关掉
                        focusManager.clearFocus()
                        attachOpen = true
                    }
                },
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
                visible = panelVisible,
                // 官方 ChatInputActionBarImplV2：入场 expandIn 150 + scaleIn 0.6→1（150）+ fadeIn 110（延迟 40）；
                // 退场 shrinkOut 110 + scaleOut→0.6（110）+ fadeOut 50；曲线统一 ease-out。
                enter = expandIn(
                    animationSpec = tween(150, easing = panelEasing),
                    clip = false,
                ) + scaleIn(
                    initialScale = 0.6f,
                    animationSpec = tween(150, easing = panelEasing),
                ) + fadeIn(
                    animationSpec = tween(110, delayMillis = 40, easing = panelEasing),
                ),
                exit = shrinkOut(
                    animationSpec = tween(110, easing = panelEasing),
                    clip = false,
                ) + scaleOut(
                    targetScale = 0.6f,
                    animationSpec = tween(110, easing = panelEasing),
                ) + fadeOut(
                    animationSpec = tween(50, easing = panelEasing),
                ),
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

        selectionText?.let { text ->
            MessageTextSelectionSheet(
                text = text,
                onDismiss = { selectionText = null },
            )
        }

        sourcesSheet?.let { list ->
            SourcesSheet(
                sources = list,
                onDismiss = { sourcesSheet = null },
                onOpen = { url -> onOpenSource(url) },
            )
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








