package com.example.deepseekclone.ui.common

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.theme.DsDimen
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 输入卡里的功能 chip（深度思考 / 智能搜索），对齐官方 2.8.0 `AppSelectableChip`：
 *
 * - 高 32dp、胶囊、左右内边距 12dp、1dp 描边、图标 16dp、图标–文字 4dp；
 * - 文字 14sp / FontWeight 500 / letterSpacing 0；
 * - **颜色由状态直接取色、瞬时切换**（官方没有 animateColorAsState）；
 * - 图标是 Lottie 形变（选中正放、取消倒放），动画时长由 JSON 决定（think 433ms / globe 500ms）。
 */
@Composable
fun DsAssistChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    icon: @Composable (Color) -> Unit = {},
) {
    val colors = DsTheme.colors
    val contentColor = if (selected) colors.chipContent else colors.textSecondary
    val background = if (selected) colors.chipBg else Color.Transparent
    val borderColor = if (selected) colors.primary.copy(alpha = 0.22f) else colors.outline
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
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            ToggleLottieIcon(
                iconRes = iconRes,
                selected = selected,
                tint = contentColor,
            )
        } else {
            Box { icon(contentColor) }
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = DsText.caption.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
            ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
