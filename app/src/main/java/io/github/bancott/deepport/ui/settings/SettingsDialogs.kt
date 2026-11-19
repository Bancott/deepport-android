package io.github.bancott.deepport.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.bancott.deepport.ui.theme.DeepSeekTheme
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

val LanguageOptions = listOf(
    "system" to "系统",
    "zh-CN" to "中文（简体中文）",
    "zh-TW" to "中文（繁體中文）",
    "en" to "English",
    "ja" to "日本語",
    "ko" to "한국어",
)

val AppearanceOptions = listOf(
    "system" to "系统",
    "light" to "浅色",
    "dark" to "深色",
)

/**
 * 官方设置弹窗：深灰圆角卡片 + 居中标题（可带图标）+ 分隔线 + 单选列表 + 分隔线 + 蓝色"确认"
 * 选中态是白色圆环 + 实心白点。
 */
@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    darkTheme: Boolean = false,
    icon: ImageVector? = null,
) {
    // 用 selected 作为 key：外部主题变化后重新打开弹窗时能正确回显当前值
    var draft by remember(selected) { mutableStateOf(selected) }

    Dialog(onDismissRequest = onDismiss) {
        // Dialog 是独立窗口：再把主题显式提供一次，避免弹窗落回 Light 默认值。
        DeepSeekTheme(darkTheme = darkTheme) {
        val colors = DsTheme.colors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.dialog),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) {
                    Spacer(Modifier.height(20.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    Spacer(Modifier.height(20.dp))
                }
                Text(
                    text = title,
                    style = DsText.h2.copy(fontSize = 19.sp),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(16.dp))
            }

            Divider()

            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clickable { draft = key }
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioDot(selected = draft == key)
                        Spacer(Modifier.width(18.dp))
                        Text(
                            text = label,
                            style = DsText.body.copy(fontSize = 17.sp),
                            color = colors.textPrimary,
                        )
                    }
                }
            }

            Divider()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clickable {
                        onSelect(draft)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "确认",
                    style = DsText.body.copy(fontSize = 17.sp),
                    color = colors.primary,
                )
            }
        }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val colors = DsTheme.colors
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(2.dp, colors.textPrimary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(colors.textPrimary),
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(DsTheme.colors.outline),
    )
}



