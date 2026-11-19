package io.github.bancott.deepport.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.bancott.deepport.ui.theme.DsDimen

/**
 * 40dp 圆形图标按钮（返回 / 关闭 / 加号 / 麦克风 / 发送）。
 * 语义标签必填，内部图标 contentDescription 传 null，避免重复播报。
 */
@Composable
fun CircleIconButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = DsDimen.IconButtonSize,
    containerColor: Color = Color.Transparent,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** 48dp 最小触控兜底：视觉 24dp，点击区撑到 48dp */
@Composable
fun MinTouchBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(DsDimen.TouchTarget),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
