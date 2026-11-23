package io.github.bancott.deepport.ui.chat

import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.SvgIcon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.ui.common.CircleIconButton
import io.github.bancott.deepport.ui.common.SendCircleGlyph
import io.github.bancott.deepport.ui.common.DeepThinkGlyph
import io.github.bancott.deepport.ui.common.DsAssistChip
import io.github.bancott.deepport.ui.common.GlobeGlyph
import io.github.bancott.deepport.ui.common.KeyboardGlyph
import io.github.bancott.deepport.ui.common.PlusCircleGlyph
import io.github.bancott.deepport.ui.common.SendArrowGlyph
import io.github.bancott.deepport.ui.common.StopSquareGlyph
import io.github.bancott.deepport.ui.common.VoiceGlyph
import io.github.bancott.deepport.ui.theme.DsDimen
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

/**
 * 官方输入卡（本项目的视觉重心）：
 * - 376dp 宽、圆角 28dp、1dp 描边、左右 12dp 外边距
 * - 第一行：文本框 / 按住说话 / 转写中
 * - 第二行：深度思考 + 智能搜索 + 加号 + 麦克风 + 发送/停止
 */
@Composable
fun DsInputBar(
    state: InputUiState,
    generating: Boolean,
    modifier: Modifier = Modifier,
    attachOpen: Boolean = false,
    editing: Boolean = false,
    onCancelEdit: () -> Unit = {},
    onTextChange: (String) -> Unit = {},
    onToggleDeepThinking: () -> Unit = {},
    onToggleSmartSearch: () -> Unit = {},
    onAttachClick: () -> Unit = {},
    onRemoveAttachment: (AttachmentUi) -> Unit = {},
    onToggleMode: () -> Unit = {},
    onDismissTranscribedNotice: () -> Unit = {},
    onVoiceStart: () -> Unit = {},
    onVoiceCancelChange: (Boolean) -> Unit = {},
    onVoiceEnd: (Boolean) -> Unit = {},
    onSend: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    val colors = DsTheme.colors
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    // 点「修改并创建分支」后输入栏自动抬起：请求焦点并唤起键盘
    LaunchedEffect(editing) {
        if (editing) {
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        if (editing) {
            EditingNotice(
                onCancel = onCancelEdit,
                modifier = Modifier.padding(horizontal = DsDimen.InputHorizontalPadding),
            )
        }
        Surface(
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DsDimen.InputHorizontalPadding),
            shape = if (editing) {
                RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp,
                    bottomStart = DsDimen.InputCornerRadius,
                    bottomEnd = DsDimen.InputCornerRadius,
                )
            } else {
                RoundedCornerShape(DsDimen.InputCornerRadius)
            },
            color = colors.surface1,
            border = BorderStroke(1.dp, colors.outline),
        ) {
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 10.dp,
                // 官方实测：卡片高 105.7 dp = 上 16 + 文本行 26 + 间距 20 + 操作行 32 + 下 12
                top = 16.dp,
                bottom = 12.dp,
            ),
        ) {

            if (state.attachments.isNotEmpty()) {
                PendingAttachmentRow(
                    items = state.attachments,
                    onRemove = onRemoveAttachment,
                )
            }

            state.transcribedNotice?.let { notice ->
                TranscribedNotice(
                    text = notice,
                    onDismiss = onDismissTranscribedNotice,
                )
                Spacer(Modifier.height(8.dp))
            }

            // 官方建议词条行：带附件且未输入文字时，文本行上方出现「这是什么 →」等提示 pills
            // （导入文件时对话框状态.jpg），1dp 描边全圆角胶囊、横向可滑
            if (state.attachments.isNotEmpty() && state.text.isEmpty()) {
                SuggestionRow(onPick = onTextChange)
                Spacer(Modifier.height(10.dp))
            }

            when {
                state.transcribing -> TranscribingRow()
                state.mode == InputMode.Voice -> HoldToTalkRow(
                    onStart = onVoiceStart,
                    onCancelChange = onVoiceCancelChange,
                    onEnd = onVoiceEnd,
                )
                else -> InputTextField(
                    text = state.text,
                    onTextChange = onTextChange,
                    focusRequester = focusRequester,
                )
            }

            Spacer(Modifier.height(20.dp))

            ActionRow(
                state = state,
                generating = generating,
                attachOpen = attachOpen,
                onToggleDeepThinking = onToggleDeepThinking,
                onToggleSmartSearch = onToggleSmartSearch,
                onAttachClick = onAttachClick,
                onToggleMode = onToggleMode,
                onSend = onSend,
                onStop = onStop,
            )
        }
        }
    }
}

/** 官方建议词条（导入文件时对话框状态.jpg）：带附件时的快捷提问 pills */
private val SuggestionPrompts = listOf("这是什么", "分析一下", "怎么办", "解释一下")

@Composable
private fun SuggestionRow(onPick: (String) -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SuggestionPrompts.forEach { prompt ->
            Row(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { onPick(prompt) }
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = prompt,
                    style = DsText.bodyTight,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "→",
                    style = DsText.bodyTight,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/**
 * 官方 MessageEditingTip（ChatInputField.kt:482 / thb.java:2326）：
 * 高 42dp、上圆角 24dp（下 4dp）、内距 14/7/9/7，
 * 内容 = 分支图标 + 「修改并创建分支」+ 28dp 圆形取消钮，文字 14sp。
 */
@Composable
private fun EditingNotice(onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(
                RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp,
                ),
            )
            .background(colors.primaryContainer)
            .padding(start = 14.dp, top = 7.dp, end = 9.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgIcon(
            R.drawable.ic_branch_outline_20,
            tint = colors.primary,
            size = 16.dp,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "修改并创建分支",
            style = DsText.bodyTight.copy(fontSize = 14.sp),
            color = colors.primary,
            modifier = Modifier.weight(1f),
        )
        CircleIconButton(
            contentDescription = "取消编辑",
            onClick = onCancel,
            size = 28.dp,
        ) {
            SvgIcon(
                R.drawable.ic_close_outline_16,
                tint = colors.textSecondary,
                size = 13.dp,
            )
        }
    }
}

@Composable
private fun InputTextField(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
) {
    val colors = DsTheme.colors
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = text,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 26.dp)
            .padding(start = 6.dp)
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                },
            )
            .onFocusChanged { focused = it.isFocused },
        onValueChange = onTextChange,
        textStyle = DsText.bodyTight.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.primary),
        maxLines = 5,
        decorationBox = { innerTextField ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        text = if (focused) "发消息" else "发消息或按住说话",
                        style = DsText.bodyTight,
                        color = colors.textTertiary,
                    )
                }
                innerTextField()
            }
        },
    )
}

/**
 * 「按住说话」：按下即开录，手指上滑超过 56dp 进入取消态，松手回调是否取消。
 * 用 awaitEachGesture 手写，避免 detectDragGestures 的长按延迟（官方是按下就录）。
 */
@Composable
private fun HoldToTalkRow(
    onStart: () -> Unit,
    onCancelChange: (Boolean) -> Unit,
    onEnd: (Boolean) -> Unit,
) {
    val colors = DsTheme.colors
    val cancelThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "按住说话",
            style = DsText.bodyTight,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        onStart()
                        var cancel = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val dragging = change.position.y - down.position.y < -cancelThreshold
                            if (dragging != cancel) {
                                cancel = dragging
                                onCancelChange(cancel)
                            }
                            change.consume()
                        }
                        onEnd(cancel)
                    }
                }
                .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun TranscribingRow() {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoiceGlyph(color = colors.primary, iconSize = 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "正在转写…",
            style = DsText.bodyTight,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun TranscribedNotice(
    text: String,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.primaryContainer)
            .padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = DsText.caption.copy(fontSize = 12.sp),
            color = colors.primary,
            modifier = Modifier.weight(1f),
        )
        CircleIconButton(
            contentDescription = "关闭转写提示",
            onClick = onDismiss,
            size = 24.dp,
        ) {
            SvgIcon(
                R.drawable.ic_close_outline_16,
                tint = colors.textSecondary,
                size = 14.dp,
            )
        }
    }
}

@Composable
private fun ActionRow(
    state: InputUiState,
    generating: Boolean,
    attachOpen: Boolean,
    onToggleDeepThinking: () -> Unit,
    onToggleSmartSearch: () -> Unit,
    onAttachClick: () -> Unit,
    onToggleMode: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DsAssistChip(
            label = "深度思考",
            selected = state.deepThinking,
            onClick = onToggleDeepThinking,
            iconRes = R.raw.chip_think,
        )

        Spacer(Modifier.width(8.dp))

        DsAssistChip(
            label = "智能搜索",
            selected = state.smartSearch,
            onClick = onToggleSmartSearch,
            iconRes = R.raw.chip_globe,
        )

        Spacer(Modifier.weight(1f))

        // 加号：面板展开时转 45° 变成叉号。
        // 官方 ChatInputAddActionButton 用的是 animateFloatAsState(target, spec = null)，
        // 即默认 spring(dampingRatio 1.0, stiffness 1500)，代码里没有毫秒数。
        val plusRotation by animateFloatAsState(
            targetValue = if (attachOpen) 45f else 0f,
            label = "attachPlusRotation",
        )
        // 官方操作行高 32dp：图标 26dp 居中，触摸盒同为 32dp（64dp 的触摸层不会撑高行）；
        // 右侧按钮中心距 44dp（32dp 盒 + 12dp 间距，官方实测 269.8/313.8/357.8 均匀分布）
        CircleIconButton(
            contentDescription = if (attachOpen) "收起附件" else "添加附件",
            onClick = onAttachClick,
            size = 32.dp,
        ) {
            Box(Modifier.rotate(plusRotation)) {
                PlusCircleGlyph(color = colors.textPrimary, iconSize = 26.dp)
            }
        }

        Spacer(Modifier.width(12.dp))

        // 语音按钮常驻第二位（官方发送态三钮并存：加号/语音/发送，导入文件时对话框状态.jpg），
        // 文本/语音模式就地切换图标。
        CircleIconButton(
            contentDescription = if (state.mode == InputMode.Text) {
                "切换到按住说话"
            } else {
                "切换到键盘输入"
            },
            onClick = onToggleMode,
            size = 32.dp,
        ) {
            if (state.mode == InputMode.Text) {
                VoiceGlyph(color = colors.textPrimary, iconSize = 26.dp)
            } else {
                KeyboardGlyph(color = colors.textPrimary, iconSize = 26.dp)
            }
        }

        // 发送：可发送时作为第三颗按钮“新增”出来——31dp 蓝色实心圆↑（官方实测 31.3dp）
        if (state.canSend && !generating) {
            Spacer(Modifier.width(12.dp))
            CircleIconButton(
                contentDescription = "发送",
                onClick = onSend,
                size = 32.dp,
            ) {
                SendCircleGlyph(
                    background = colors.primary,
                    arrowColor = colors.onPrimary,
                    iconSize = 31.dp,
                )
            }
        }

        // 生成中：第三颗位置换成停止（30dp 实心圆）
        if (generating) {
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                SendOrStopButton(
                    generating = true,
                    enabled = true,
                    onSend = onSend,
                    onStop = onStop,
                )
            }
        }
    }
}

@Composable
private fun SendOrStopButton(
    generating: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = DsTheme.colors
    val label = if (generating) "停止生成" else "发送"
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.primary else colors.primary.copy(alpha = 0.35f))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
                onClick = { if (generating) onStop() else onSend() },
            )
            .semantics { contentDescription = label }
            .padding(0.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (generating) {
            StopSquareGlyph(color = Color.White, iconSize = 15.dp)
        } else {
            SendArrowGlyph(color = Color.White, iconSize = 14.dp)
        }
    }
}















