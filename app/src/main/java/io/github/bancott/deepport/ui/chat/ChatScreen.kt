package io.github.bancott.deepport.ui.chat

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.chat.web.WebChatList
import io.github.bancott.deepport.ui.common.CircleIconButton
import io.github.bancott.deepport.ui.common.DeepSeekTopBar
import io.github.bancott.deepport.ui.common.NewChatGlyph
import io.github.bancott.deepport.ui.common.SpeakerGlyph
import io.github.bancott.deepport.ui.common.SpeakerWaveGlyph
import io.github.bancott.deepport.ui.theme.DeepSeekTheme
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

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
    /** 抽屉是否展开：展开时收起键盘与附件面板，避免输入栏顶起会话 */
    drawerOpen: Boolean = false,
    onNewChatClick: () -> Unit = {},
    onToggleSpeechOutput: () -> Unit = {},
    /** 朗读中再点顶栏按钮：立即停止朗读 */
    onStopSpeaking: () -> Unit = {},
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
    /// 选区朗读：系统选择工具栏里的「朗读」，走 App 内置 TTS
    onSpeakSelection: (String, String?) -> Unit = { _, _ -> },
    /** 网页里右滑（代码块已到最左端时）请求打开历史 */
    /** 消息区手势与顶栏共用同一份进度 */
    drawerProgress: androidx.compose.runtime.MutableFloatState? = null,
    /** 抽屉行程（px）：手指位移换算成进度用 */
    drawerTravelPx: Float = 1f,
    /** 消息区松手结算（true=打开） */
    onDrawerSettle: (Boolean) -> Unit = {},
    /** 消息区开始拖动：取消正在跑的吸附动画 */
    onDrawerDragStart: () -> Unit = {},
    /** 自增一次 = 清掉网页里的选区（朗读读完） */
    clearSelectionTick: Int = 0,
    onShareMessage: (MessageUi) -> Unit = {},
    onRegenerate: (MessageUi) -> Unit = {},
    /** 终止后「继续生成」：从停止处接着写 */
    onContinueMessage: (MessageUi) -> Unit = {},
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

    // 居中的轻提示（新对话重复点击 / 朗读开关）
    var toastText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastText) {
        if (toastText != null) {
            delay(1600)
            toastText = null
        }
    }
    var scrollToTopTick by remember { mutableIntStateOf(0) }
    // 附件面板：点「+」展开，键盘弹起 / 发出消息后自动收起
    var attachOpen by remember { mutableStateOf(false) }
    var sourcesSheet by remember { mutableStateOf<List<SourceItem>?>(null) }
    // 用户消息长按菜单：消息 + 长按点在消息区里的位置（CSS px）
    var userMenu by remember { mutableStateOf<Pair<MessageUi, Offset>?>(null) }
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
    // 向右滑出历史会话（或点菜单键）时，如果输入栏正被键盘/附件面板顶着，一并收起
    LaunchedEffect(drawerOpen) {
        if (drawerOpen) {
            attachOpen = false
            focusManager.clearFocus()
        }
    }

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
    // 键盘留白补偿：只在「面板展开中」用锁存的键盘高度做过渡（否则输入栏会先掉下去再被面板顶上来）；
    // 关闭时 keyboard 已经收起，必须按当前 ime（=0）算，否则会把几百 px 的留白重新加回来，
    // 表现就是收起面板时输入栏先往上弹一下。
    val padSourcePx = if (panelVisible) maxOf(lastNonZeroIme, imeBottomPx) else imeBottomPx
    val inputBottomPadding = with(density) {
        val gapPx = 8.dp.toPx()
        val extra = (padSourcePx - disclaimerBlockPx).coerceAtLeast(0f)
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
                onTitleDoubleTap = { scrollToTopTick++ },
            ) {
                // 顶栏朗读按钮：朗读中显示喇叭 + 两道声波呼吸（与消息动作条同一套节奏），
                // 空闲时仍是官方三态喇叭图标。
                val reading = state.readingState != ReadAloudState.Idle
                val speechOn = state.isSpeechOutputOn
                CircleIconButton(
                    contentDescription = if (speechOn) "关闭自动朗读" else "开启自动朗读",
                    ripple = false,
                    onClick = {
                        if (reading) {
                            // 朗读中：一次点击就停，而不是先切开关
                            onStopSpeaking()
                        } else {
                            onToggleSpeechOutput()
                            toastText = if (speechOn) "已关闭自动阅读" else "已开启自动阅读"
                        }
                    },
                ) {
                    if (reading) {
                        SpeakerWaveGlyph(color = colors.primary, iconSize = 24.dp)
                    } else {
                        // 官方图标：关闭 = mute 位图（自带斜线），开启 = play 位图；动效暂时不做
                        SpeakerGlyph(
                            color = colors.textPrimary,
                            muted = !speechOn,
                            reading = false,
                            iconSize = 24.dp,
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                CircleIconButton(
                    contentDescription = "新建对话",
                    onClick = {
                        if (state.isEmpty) toastText = "已经在新对话中" else onNewChatClick()
                    },
                ) {
                    NewChatGlyph(colors.textPrimary, iconSize = 24.dp)
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
                    // 批次 A：整个消息区换成一个 WebView，网页自己滚动（不再有多个 WebView 抢手势）
                    WebChatList(
                        messages = state.messages,
                        sessionTokens = state.sessionTokens,
                        drawerProgress = drawerProgress,
                        drawerTravelPx = drawerTravelPx,
                        drawerOpen = drawerOpen,
                        onSettle = onDrawerSettle,
                        onDragStart = onDrawerDragStart,
                        clearSelectionTick = clearSelectionTick,
                        readingMessageId = state.readingMessageId,
                        onSpeakSelection = onSpeakSelection,
                        generating = state.isGenerating,
                        scrollToTopTick = scrollToTopTick,
                        onMessageAction = { m, action ->
                            when (action) {
                                "continue" -> onContinueMessage(m)
                                "copy" -> onCopyMessage(m)
                                "speak" -> onSpeakMessage(m)
                                "share" -> onShareMessage(m)
                                "regenerate" -> onRegenerate(m)
                                "retry" -> onRetryMessage(m)
                                "branch-prev" -> onBranchPrev(m)
                                "branch-next" -> onBranchNext(m)
                                "reasoning" -> onToggleReasoning(m)
                                // 回答下方的「N 个网页」：打开原生来源列表
                                "sources" -> sourcesSheet = m.sources
                            }
                        },
                        onUserMessageMenu = { m, x, y -> userMenu = m to Offset(x, y) },
                    )
                    // 消息区底部不加渐隐：官方是输入栏自己带一条硬边背景，不是渐变
                    // 用户消息长按菜单：锚在气泡上沿（网页给的 CSS px → dp）
                    userMenu?.let { (msg, at) ->
                        // 网页里的 clientX/Y 是 CSS px（= dp），不能再除以 density
                        val anchorX = at.x.dp
                        val anchorY = at.y.dp
                        MessageMenuPopup(
                            message = msg,
                            expanded = true,
                            onExpandedChange = { if (!it) userMenu = null },
                            anchorX = anchorX,
                            anchorY = anchorY,
                            onRegenerate = {
                                userMenu = null
                                onRegenerate(msg)
                            },
                            onEdit = {
                                userMenu = null
                                onEditMessage(msg)
                            },
                            showSelectText = false,
                        )
                    }
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
                // 发送后如果输入栏是被顶起来的状态（键盘弹着 / 附件面板展开），先收起再发，
                // 让消息列表立刻占满下半屏，而不是停在半截。
                onSend = {
                    if (attachOpen) attachOpen = false
                    focusManager.clearFocus()
                    onSend()
                },
                onStop = onStop,
            )

            AnimatedVisibility(
                visible = panelVisible,
                // 只要「高度展开/收起」：面板从输入栏下方长出来，把输入栏自然顶上去；
                // 关闭时按同一路径收回，不做缩放/横向生长，避免三个块像从左往右冒出来。
                enter = expandVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Top,
                ) + fadeIn(tween(120)),
                exit = shrinkVertically(
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(tween(100)),
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

            // 附件面板展开 / 录音中让位（官方这两块内容都不显示）。
            // 免责声明的收放和面板用同一组时长（展开 220 / 收起 200），两条高度曲线形状一致，
            // 叠在一起时输入栏只会单调移动，不会先向下再被顶上去。
            AnimatedVisibility(
                visible = !attachOpen && !voiceRecording,
                enter = fadeIn(tween(120)) + expandVertically(tween(200, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(220, easing = FastOutSlowInEasing)),
            ) {
                Text(
                    text = "回答由 AI 生成，仅供参考",
                    style = DsText.meta.copy(fontSize = 12.sp),
                    color = if (state.isEmpty) androidx.compose.ui.graphics.Color.Transparent else colors.textTertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DisclaimerBlockTopPadding, bottom = DisclaimerBlockBottomPadding),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // 按住说话：整屏泛光 + 提示 + 波形（盖在最上层；纯绘制，不拦触摸）
        if (voiceRecording) {
            VoiceOverlay(cancel = voiceCancel, levels = voiceLevels)
        }

        toastText?.let { text ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = DsText.bodyTight,
                    color = colors.textPrimary,
                    modifier = Modifier
                        .background(colors.surface2, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
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
    // 官方首页的鲸鱼：点空白处游一次；这里再自动游一次，进新对话就能看到
    var playKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(350)
        playKey++
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { playKey++ },
    ) {
        HomeWhale(
            color = colors.primary,
            playKey = playKey,
            modifier = Modifier.matchParentSize(),
        )
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








