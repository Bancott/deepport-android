package com.example.deepseekclone.ui.chat

import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.SvgIcon
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.SendCircleGlyph
import com.example.deepseekclone.ui.common.DeepThinkGlyph
import com.example.deepseekclone.ui.common.DsAssistChip
import com.example.deepseekclone.ui.common.GlobeGlyph
import com.example.deepseekclone.ui.common.KeyboardGlyph
import com.example.deepseekclone.ui.common.PlusCircleGlyph
import com.example.deepseekclone.ui.common.SendArrowGlyph
import com.example.deepseekclone.ui.common.StopSquareGlyph
import com.example.deepseekclone.ui.common.VoiceGlyph
import com.example.deepseekclone.ui.theme.DsDimen
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

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
    Surface(
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DsDimen.InputHorizontalPadding),
        shape = RoundedCornerShape(DsDimen.InputCornerRadius),
        color = colors.surface1,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 10.dp,
                top = 14.dp,
                bottom = 10.dp,
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
                )
            }

            Spacer(Modifier.height(15.dp))

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

@Composable
private fun InputTextField(
    text: String,
    onTextChange: (String) -> Unit,
) {
    val colors = DsTheme.colors
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 26.dp)
            .padding(start = 6.dp),
        textStyle = DsText.bodyTight.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.primary),
        maxLines = 5,
        decorationBox = { innerTextField ->
            Box {
                if (text.isEmpty()) {
                    Text(
                        text = "发消息或按住说话",
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DsAssistChip(
            label = "深度思考",
            selected = state.deepThinking,
            onClick = onToggleDeepThinking,
            iconRes = R.raw.chip_think,
        )

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
        // 官方触摸盒 64×64dp（图标 26dp）
        CircleIconButton(
            contentDescription = if (attachOpen) "收起附件" else "添加附件",
            onClick = onAttachClick,
            size = 64.dp,
        ) {
            Box(Modifier.rotate(plusRotation)) {
                PlusCircleGlyph(color = colors.textPrimary, iconSize = 26.dp)
            }
        }

        // 第二个位置：有文字时"语音按钮"就地变成等大的上传（发送）按钮；
        // 没有文字时是麦克风 / 语音模式下是键盘。
        if (state.canSend && !generating) {
            CircleIconButton(
                contentDescription = "发送",
                onClick = onSend,
            ) { SendCircleGlyph(background = colors.primary, arrowColor = colors.onPrimary, iconSize = 24.6.dp) }
        } else {
            CircleIconButton(
                contentDescription = if (state.mode == InputMode.Text) {
                    "切换到按住说话"
                } else {
                    "切换到键盘输入"
                },
                onClick = onToggleMode,
            ) {
                if (state.mode == InputMode.Text) {
                    VoiceGlyph(color = colors.textPrimary, iconSize = 26.dp)
                } else {
                    KeyboardGlyph(color = colors.textPrimary, iconSize = 26.dp)
                }
            }
        }

        // 第三个位置：模型开始工作后才"新增"出来的暂停按钮（包在 40dp 盒里，与左侧按钮同间距）
        if (generating) {
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
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















