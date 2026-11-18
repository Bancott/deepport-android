package com.example.deepseekclone.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.theme.DsDimen
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 输入卡里的功能 chip（深度思考 / 智能搜索）。
 * 选中：浅蓝底 + 品牌蓝字；未选中：透明底 + 1dp 描边 + 次级灰。
 */
@Composable
fun DsAssistChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (Color) -> Unit = {},
) {
    val colors = DsTheme.colors
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.chipContent else colors.textSecondary,
        animationSpec = tween(200),
        label = "chipContent",
    )
    val background by animateColorAsState(
        targetValue = if (selected) colors.chipBg else Color.Transparent,
        animationSpec = tween(200),
        label = "chipBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.22f) else colors.outline,
        animationSpec = tween(200),
        label = "chipBorder",
    )
    // 官方点击 chip 时图标有一个短促的旋转/回弹；用 90° 旋转 + 轻微回缩，
    // 比单纯换色更能让“已开启”被看见。
    val iconProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        label = "chipIcon",
    )
    Row(
        modifier = modifier
            .height(DsDimen.ChipHeight)
            .clip(CircleShape)
            .background(background)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape,
            )
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                rotationZ = 90f * iconProgress
                val scale = 1f - 0.06f * (1f - iconProgress)
                scaleX = scale
                scaleY = scale
            },
        ) {
            icon(contentColor)
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = DsText.caption.copy(fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}



