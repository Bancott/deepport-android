package com.example.deepseekclone.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.SvgIcon
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/** 官方开关是绿色（#34C759 左右），与主题蓝区分 */
private val SwitchGreen = Color(0xFF2FCC71)

@Composable
fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = DsTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgGrouped)
            .statusBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            CircleIconButton(
                contentDescription = "返回",
                onClick = onBack,
                containerColor = colors.surface2,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                style = DsText.h2.copy(fontSize = 18.sp),
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = DsText.caption,
        color = DsTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Card(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = DsTheme.colors.surface2,
        content = { Column(content = content) },
    )
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(DsTheme.colors.outline),
    )
}

@Composable
private fun RowItem(
    title: String,
    value: String? = null,
    titleColor: Color? = null,
    leading: (@Composable () -> Unit)? = null,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            it()
            Spacer(Modifier.width(14.dp))
        }
        Text(
            text = title,
            style = DsText.label.copy(fontSize = 16.sp),
            color = titleColor ?: colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
        if (value != null) {
            Text(
                text = value,
                style = DsText.caption.copy(fontSize = 14.sp),
                color = colors.textSecondary,
            )
        }
        if (showChevron && onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun GreenSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = SwitchGreen,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = DsTheme.colors.surface3,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun Caption(text: String) {
    Text(
        text = text,
        style = DsText.caption.copy(fontSize = 14.sp, lineHeight = 22.sp),
        color = DsTheme.colors.textSecondary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
    )
}

/* ------------------------------ 账号管理 ------------------------------ */

@Composable
fun AccountScreen(onBack: () -> Unit) {
    val colors = DsTheme.colors
    SettingsSubPage("账号管理", onBack) {
        SectionLabel("账户")
        Card {
            RowItem(
                title = "手机号",
                value = "182******20",
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = {},
            )
            Divider()
            RowItem(
                title = "电子邮箱",
                value = "ck****5@qq.com",
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
            Divider()
            RowItem(
                title = "微信",
                value = "已绑定",
                leading = {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF07C160)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("微", style = DsText.caption.copy(fontSize = 11.sp), color = Color.White)
                    }
                },
                onClick = {},
            )
        }

        Spacer(Modifier.height(12.dp))
        Card {
            RowItem(title = "未成年人模式", value = "已关闭", onClick = {})
        }

        SectionLabel("安全")
        Card {
            RowItem(
                title = "登录设备管理",
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.Smartphone,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                },
                onClick = {},
            )
        }

        Spacer(Modifier.height(60.dp))
        Text(
            text = "注销账号",
            style = DsText.label.copy(fontSize = 15.sp),
            color = colors.danger,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(vertical = 16.dp),
        )
    }
}

/* ------------------------------ 数据管理 ------------------------------ */

@Composable
fun DataScreen(onBack: () -> Unit) {
    var optimize by remember { mutableStateOf(true) }
    SettingsSubPage("数据管理", onBack) {
        Card {
            RowItem(
                title = "数据用于优化体验",
                showChevron = false,
                trailing = { GreenSwitch(optimize) { optimize = it } },
            )
        }
        Caption("允许我们将你的对话内容用于优化 DeepSeek 的使用体验。我们保障你的数据隐私安全。")

        Spacer(Modifier.height(8.dp))
        Card { RowItem(title = "我共享的链接", onClick = {}) }

        Spacer(Modifier.height(12.dp))
        Card { RowItem(title = "导出所有历史对话", onClick = {}) }

        Spacer(Modifier.height(12.dp))
        Card {
            RowItem(title = "删除所有历史对话", titleColor = DsTheme.colors.danger, onClick = {})
        }
    }
}

/* ------------------------------ 个性化 ------------------------------ */

@Composable
fun PersonalizationScreen(onBack: () -> Unit) {
    var autoCollapse by remember { mutableStateOf(true) }
    SettingsSubPage("个性化", onBack) {
        Card {
            RowItem(
                title = "思考内容自动折叠",
                showChevron = false,
                trailing = { GreenSwitch(autoCollapse) { autoCollapse = it } },
            )
        }
        Caption("开启后，思考结束后将自动折叠思考内容")
    }
}

/* ------------------------------ 字体大小 ------------------------------ */

@Composable
fun FontSizeScreen(
    current: Float,
    onBack: () -> Unit,
    onSelect: (Float) -> Unit,
) {
    val colors = DsTheme.colors
    val steps = listOf(
        0.85f to "小",
        0.925f to "较小",
        1.0f to "标准",
        1.1f to "较大",
        1.2f to "大",
        1.3f to "特大",
    )
    var followSystem by remember { mutableStateOf(current == 0f) }
    var index by remember {
        val found = steps.indexOfFirst { kotlin.math.abs(it.first - current) < 0.01f }
        mutableStateOf(if (found >= 0) found else 2)
    }
    val colors2 = colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors2.bg)
            .statusBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            CircleIconButton(
                contentDescription = "返回",
                onClick = onBack,
                containerColor = colors2.surface2,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = null,
                    tint = colors2.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "字体大小",
                style = DsText.h2.copy(fontSize = 18.sp),
                color = colors2.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // 预览区：一条用户气泡 + 两段正文
        Column(Modifier.weight(1f).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors2.bubble)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text("预览文字大小", style = DsText.body, color = colors2.textPrimary)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "你可以拖动滑块来调整字体大小。",
                style = DsText.body,
                color = colors2.textPrimary,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "如果在使用过程中存在问题或建议，可反馈给 DeepSeek 团队。",
                style = DsText.body,
                color = colors2.textPrimary,
            )
        }

        // 底部面板
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = colors2.surface2,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "跟随系统",
                        style = DsText.label.copy(fontSize = 16.sp),
                        color = colors2.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    GreenSwitch(followSystem) { followSystem = it }
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("A", style = DsText.caption.copy(fontSize = 14.sp), color = colors2.textPrimary)
                    androidx.compose.material3.Slider(
                        value = index.toFloat(),
                        onValueChange = {
                            index = it.toInt()
                            followSystem = false
                            onSelect(steps[index].first)
                        },
                        valueRange = 0f..(steps.size - 1).toFloat(),
                        steps = steps.size - 2,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = colors2.textTertiary,
                            inactiveTrackColor = colors2.outline,
                        ),
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    )
                    Text("A", style = DsText.h2.copy(fontSize = 20.sp), color = colors2.textPrimary)
                }

                Text(
                    text = if (followSystem) "跟随系统" else steps[index].second,
                    style = DsText.caption.copy(fontSize = 12.sp),
                    color = colors2.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}

