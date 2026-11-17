package com.example.deepseekclone.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.chat.markdown.MarkdownText
import com.example.deepseekclone.ui.common.GlobeGlyph
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/** 会话消息流：LazyColumn + 稳定 key，用户气泡与 AI 通栏回答两种形态。 */
@Composable
fun MessageStream(
    messages: List<MessageUi>,
    sessionTokens: Int,
    modifier: Modifier = Modifier,
    onToggleReasoning: (MessageUi) -> Unit = {},
    onCopy: (MessageUi) -> Unit = {},
    onSpeak: (MessageUi) -> Unit = {},
    onShare: (MessageUi) -> Unit = {},
    onRegenerate: (MessageUi) -> Unit = {},
    onRetry: (MessageUi) -> Unit = {},
    onBranchPrev: (MessageUi) -> Unit = {},
    onBranchNext: (MessageUi) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 10.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            when (message.role) {
                ChatRole.User -> UserMessage(message)
                ChatRole.Assistant -> AssistantMessage(
                    message = message,
                    sessionTokens = sessionTokens,
                    onToggleReasoning = { onToggleReasoning(message) },
                    onCopy = { onCopy(message) },
                    onSpeak = { onSpeak(message) },
                    onShare = { onShare(message) },
                    onRegenerate = { onRegenerate(message) },
                    onRetry = { onRetry(message) },
                    onBranchPrev = { onBranchPrev(message) },
                    onBranchNext = { onBranchNext(message) },
                )
            }
        }
    }
}

/* ----------------------------- 用户气泡 ----------------------------- */

@Composable
private fun UserMessage(message: MessageUi) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp, top = 14.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            color = colors.bubble,
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = message.text,
                style = DsText.bodyTight,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

/* ----------------------------- AI 回答 ----------------------------- */

@Composable
private fun AssistantMessage(
    message: MessageUi,
    sessionTokens: Int,
    onToggleReasoning: () -> Unit,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
    onBranchPrev: () -> Unit,
    onBranchNext: () -> Unit,
) {
    val colors = DsTheme.colors
    val streaming = message.status == MessageStatus.Streaming || message.status == MessageStatus.Reasoning
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp),
    ) {
        if (message.reasoning.isNotBlank() || message.status == MessageStatus.Reasoning) {
            ReasoningSection(
                message = message,
                onToggle = onToggleReasoning,
            )
            if (message.text.isNotEmpty()) {
                Spacer(Modifier.height(if (message.reasoningExpanded) 14.dp else 8.dp))
            }
        }

        when (message.status) {
            MessageStatus.Connecting -> TypingDots()
            MessageStatus.Failed -> ErrorTurn(hint = message.errorHint ?: "请检查网络后重试", onRetry = onRetry)
            else -> {
                if (message.text.isNotEmpty()) {
                    MarkdownText(
                        markdown = message.text,
                        color = colors.textPrimary,
                    )
                }
            }
        }

        if (message.status == MessageStatus.Aborted) {
            Text(
                text = "已停止生成",
                style = DsText.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        if (message.status == MessageStatus.Completed || message.status == MessageStatus.Aborted) {
            if (message.sourcesCount > 0) {
                Spacer(Modifier.height(14.dp))
                SourcesPill(count = message.sourcesCount)
            }
            MessageActionBar(
                message = message,
                sessionTokens = sessionTokens,
                onCopy = onCopy,
                onSpeak = onSpeak,
                onShare = onShare,
                onRegenerate = onRegenerate,
                onBranchPrev = onBranchPrev,
                onBranchNext = onBranchNext,
            )
        }
    }
}

@Composable
private fun ReasoningSection(message: MessageUi, onToggle: () -> Unit) {
    val colors = DsTheme.colors
    val streaming = message.status == MessageStatus.Reasoning
    val headerColor = if (streaming) colors.textPrimary else colors.textSecondary
    val headerText = if (streaming) {
        "正在思考"
    } else {
        "已思考（用时 ${message.reasoningSeconds} 秒）"
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = headerText,
                style = DsText.bodyTight,
                color = headerColor,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (message.reasoningExpanded) {
                    Icons.Outlined.KeyboardArrowUp
                } else {
                    Icons.Outlined.KeyboardArrowRight
                },
                contentDescription = if (message.reasoningExpanded) "收起思考过程" else "展开思考过程",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        if (message.reasoningExpanded && message.reasoning.isNotBlank()) {
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(colors.outline),
                )
                Text(
                    text = message.reasoning,
                    style = DsText.bodyTight,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 三个点的等待指示（官方 Connecting 态） */
@Composable
fun TypingDots(modifier: Modifier = Modifier) {
    val colors = DsTheme.colors
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = modifier.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 520,
                        delayMillis = index * 140,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.textTertiary.copy(alpha = alpha)),
            )
        }
    }
}

/** 失败态：橘色重试圆钮 + 提示文案（右对齐，与官方一致） */
@Composable
private fun ErrorTurn(hint: String, onRetry: () -> Unit) {
    val colors = DsTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.warning)
                .clickable(role = Role.Button, onClick = onRetry),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "重试",
                tint = Color(0xFF1B1B1B),
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = hint,
            style = DsText.caption.copy(fontSize = 13.sp),
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SourcesPill(count: Int) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surface2)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlobeGlyph(color = colors.textSecondary, iconSize = 16.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$count 个网页",
            style = DsText.caption.copy(fontSize = 13.sp),
            color = colors.textSecondary,
        )
    }
}

/* --------------------------- 动作条 / token --------------------------- */

@Composable
private fun MessageActionBar(
    message: MessageUi,
    sessionTokens: Int,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onBranchPrev: () -> Unit,
    onBranchNext: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (message.branchCount > 1) {
            ActionIcon(
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowLeft,
                        contentDescription = "上一条分支",
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = "上一条分支",
                onClick = onBranchPrev,
            )
            Text(
                text = "${message.branchIndex}/${message.branchCount}",
                style = DsText.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 2.dp),
            )
            ActionIcon(
                icon = { tint ->
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowRight,
                        contentDescription = "下一条分支",
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = "下一条分支",
                onClick = onBranchNext,
            )
            Spacer(Modifier.width(10.dp))
        }

        ActionIcon(
            icon = { tint ->
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            },
            label = "复制",
            onClick = onCopy,
        )
        ActionIcon(
            icon = { tint ->
                Icon(
                    imageVector = Icons.Outlined.VolumeUp,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            },
            label = "朗读",
            onClick = onSpeak,
        )
        ActionIcon(
            icon = { tint ->
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            },
            label = "分享",
            onClick = onShare,
        )

        Spacer(Modifier.weight(1f))

        TokenInline(message = message, sessionTokens = sessionTokens)

        ActionIcon(
            icon = { tint ->
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            },
            label = "重新生成",
            onClick = onRegenerate,
        )
    }
}

@Composable
private fun ActionIcon(
    icon: @Composable (Color) -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        icon(colors.textSecondary)
    }
}

@Composable
private fun TokenLine(message: MessageUi, sessionTokens: Int, estimating: Boolean = false) {
    val colors = DsTheme.colors
    val usage = message.usage
    val prefix = if (estimating) "≈" else ""
    val text = if (usage == null) {
        null
    } else {
        val up = formatTokens(usage.prompt)
        val down = formatTokens(usage.completion)
        val session = formatTokens(sessionTokens)
        "$prefix↑$up ↓$down · 会话 $session"
    }
    if (text != null) {
        Text(
            text = text,
            style = DsText.meta.copy(fontSize = 11.5.sp),
            color = colors.textSecondary.copy(alpha = if (estimating) 0.6f else 0.9f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.End,
        )
    } else {
        Spacer(Modifier.height(4.dp))
    }
}

internal fun formatTokens(value: Int): String = when {
    value < 1000 -> value.toString()
    value < 10_000 -> String.format("%.1fk", value / 1000f)
    else -> "${value / 1000}k"
}

/** 内容滚到输入栏上方时做一点渐隐，避免文字硬切（官方同款观感） */
@Composable
fun BottomFadeMask(modifier: Modifier = Modifier, height: Int = 28) {
    val colors = DsTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, colors.bg),
                ),
            ),
    )
}



/** token 用量直接排在动作条里（复制之后、朗读之前），字号与图标行匹配 */
@Composable
private fun TokenInline(message: MessageUi, sessionTokens: Int) {
    val usage = message.usage ?: return
    val colors = DsTheme.colors
    val text = buildString {
        append("↑")
        append(formatTokens(usage.prompt))
        append(" ↓")
        append(formatTokens(usage.completion))
        if (sessionTokens > 0) {
            append(" · ")
            append(formatTokens(sessionTokens))
        }
    }
    Text(
        text = text,
        style = DsText.caption.copy(fontSize = 13.sp),
        color = colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}


