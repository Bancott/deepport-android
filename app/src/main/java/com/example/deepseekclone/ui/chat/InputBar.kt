package com.example.deepseekclone.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.DeepThinkGlyph
import com.example.deepseekclone.ui.common.DsAssistChip
import com.example.deepseekclone.ui.common.GlobeGlyph
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
    onTextChange: (String) -> Unit = {},
    onToggleDeepThinking: () -> Unit = {},
    onToggleSmartSearch: () -> Unit = {},
    onAttachClick: () -> Unit = {},
    onToggleMode: () -> Unit = {},
    onDismissTranscribedNotice: () -> Unit = {},
    onHoldToTalk: () -> Unit = {},
    onSend: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    val colors = DsTheme.colors
    Surface(
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
            state.transcribedNotice?.let { notice ->
                TranscribedNotice(
                    text = notice,
                    onDismiss = onDismissTranscribedNotice,
                )
                Spacer(Modifier.height(8.dp))
            }

            when {
                state.transcribing -> TranscribingRow()
                state.mode == InputMode.Voice -> HoldToTalkRow(onHoldToTalk = onHoldToTalk)
                else -> InputTextField(
                    text = state.text,
                    onTextChange = onTextChange,
                )
            }

            Spacer(Modifier.height(15.dp))

            ActionRow(
                state = state,
                generating = generating,
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

@Composable
private fun HoldToTalkRow(onHoldToTalk: () -> Unit) {
    val colors = DsTheme.colors
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
                .clickable(onClick = onHoldToTalk)
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
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun ActionRow(
    state: InputUiState,
    generating: Boolean,
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
        ) { tint -> DeepThinkGlyph(color = tint, iconSize = 14.dp) }

        DsAssistChip(
            label = "智能搜索",
            selected = state.smartSearch,
            onClick = onToggleSmartSearch,
        ) { tint -> GlobeGlyph(color = tint, iconSize = 18.3.dp) }

        Spacer(Modifier.weight(1f))

        CircleIconButton(
            contentDescription = "添加附件",
            onClick = onAttachClick,
        ) { PlusCircleGlyph(color = colors.textPrimary, iconSize = 25.dp) }

        // 右侧只保留一个 40dp 圆形按钮：
        // 空输入=麦克风 / 语音模式=键盘 / 有内容或生成中=发送或停止（与官方一致，不是并排三个）
        if (generating || state.canSend) {
            SendOrStopButton(
                generating = generating,
                enabled = generating || state.canSend,
                onSend = onSend,
                onStop = onStop,
            )
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
                    VoiceGlyph(color = colors.textPrimary, iconSize = 25.dp)
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Keyboard,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
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
            StopSquareGlyph(color = Color.White, iconSize = 9.dp)
        } else {
            SendArrowGlyph(color = Color.White, iconSize = 13.dp)
        }
    }
}







