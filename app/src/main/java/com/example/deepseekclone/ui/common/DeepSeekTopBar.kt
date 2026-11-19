package com.example.deepseekclone.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.deepseekclone.ui.theme.DsDimen
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 全站统一顶栏：左菜单/返回 + 左对齐标题 + 右侧动作组。
 * 高度 56dp，左右内边距 6dp（让 40dp 圆形按钮的视觉留白与截图一致）。
 */
@Composable
fun DeepSeekTopBar(
    title: String,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    menuDescription: String = "打开会话列表",
    menuContent: @Composable () -> Unit = {
        MenuGlyph(color = DsTheme.colors.textPrimary)
    },
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = DsTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(DsDimen.TopBarHeight)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            contentDescription = menuDescription,
            onClick = onMenuClick,
            // 官方顶栏导航键（开/关侧边栏）触摸区 44dp
            size = 44.dp,
            content = menuContent,
        )
        Spacer(Modifier.width(8.dp))
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = DsText.titleBar,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        actions()
    }
}
