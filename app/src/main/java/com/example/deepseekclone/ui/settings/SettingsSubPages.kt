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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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
                SvgIcon(
                    R.drawable.ic_chevron_left_outline_20,
                    tint = colors.textPrimary,
                    size = 20.dp,
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
            SvgIcon(
                R.drawable.ic_chevron_right_outline_20,
                tint = colors.textSecondary,
                size = 20.dp,
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
fun AccountScreen(
    userName: String,
    userAvatar: String,
    onBack: () -> Unit,
) {
    val colors = DsTheme.colors
    val loggedIn = userName.isNotBlank() || userAvatar.isNotBlank()
    SettingsSubPage("账号管理", onBack) {
        SectionLabel("账户")
        Card {
            RowItem(
                title = "当前账号",
                value = if (loggedIn) userName.ifBlank { "已登录" } else "未登录",
                leading = {
                    SvgIcon(
                        R.drawable.ic_user_regular,
                        tint = colors.textPrimary,
                        size = 22.dp,
                    )
                },
                showChevron = false,
            )
            Divider()
            RowItem(
                title = "账号体系",
                value = "本地复刻版",
                leading = {
                    SvgIcon(
                        R.drawable.ic_info_outline,
                        tint = colors.textPrimary,
                        size = 22.dp,
                    )
                },
                showChevron = false,
            )
        }
        Caption(
            "这个复刻版不接入官方账号体系，也没有手机号/微信登录。" +
                "这里只显示官方充值页登录后缓存的昵称，未登录时不会伪造账号信息。",
        )
    }
}

/* ------------------------------ 数据管理 ------------------------------ */

@Composable
fun DataScreen(
    onBack: () -> Unit,
    onShareCurrent: () -> String? = { null },
    onExportAll: () -> Unit = {},
    onDeleteAll: () -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    // "数据用于优化体验"已移除：这是自建壳，没有官方后端，这个开关没有任何实际作用。
    SettingsSubPage("数据管理", onBack) {
        Card {
            // 官方这里是「我共享的链接」（服务端托管的 URL）。没有后端生成不了真链接，
            // 改成把当前对话交给系统分享面板（文本/邮件/IM 都能接）。
            RowItem(
                title = "分享当前对话",
                onClick = {
                    val text = onShareCurrent()
                    if (text.isNullOrBlank()) {
                        android.widget.Toast.makeText(context, "当前还没有可分享的内容", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "分享对话"))
                    }
                },
            )
        }

        Spacer(Modifier.height(12.dp))
        Card { RowItem(title = "导出所有历史对话", onClick = onExportAll) }

        Spacer(Modifier.height(12.dp))
        Card {
            RowItem(
                title = "删除所有历史对话",
                titleColor = DsTheme.colors.danger,
                onClick = { confirmDelete = true },
            )
        }
    }

    if (confirmDelete) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除所有历史对话？", color = DsTheme.colors.textPrimary) },
            text = { Text("删除后无法恢复，当前会话也会一并清空。", color = DsTheme.colors.textSecondary) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        confirmDelete = false
                        onDeleteAll()
                    },
                ) {
                    Text("删除", color = DsTheme.colors.danger)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) {
                    Text("取消", color = DsTheme.colors.textSecondary)
                }
            },
            containerColor = DsTheme.colors.dialog,
        )
    }
}

/* ------------------------------ 个性化 ------------------------------ */

@Composable
fun PersonalizationScreen(
    autoCollapse: Boolean,
    onAutoCollapseChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    SettingsSubPage("个性化", onBack) {
        Card {
            RowItem(
                title = "思考内容自动折叠",
                showChevron = false,
                trailing = { GreenSwitch(autoCollapse, onAutoCollapseChange) },
            )
        }
        Caption("开启后，思考结束后将自动折叠思考内容")
    }
}

/* ------------------------------ 字体大小 ------------------------------ */

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    // 预览区按当前档位缩放，页面其他部分保持 1 倍
    val previewScale = if (followSystem) 1f else steps[index].first
    // 整页的字号锁成 1 倍：调档时不希望下面的档位卡片一起变大变小，只有上方示例需要缩放
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides Density(LocalDensity.current.density, 1f),
    ) {
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
                SvgIcon(
                    R.drawable.ic_chevron_left_outline_20,
                    tint = colors2.textPrimary,
                    size = 20.dp,
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
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides Density(LocalDensity.current.density, previewScale),
            ) {
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
                    StepSlider(
                        index = index,
                        count = steps.size,
                        onChange = { i ->
                            if (i != index) {
                                index = i
                                followSystem = false
                                onSelect(steps[i].first)
                            }
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
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
}


/**
 * 自绘档位条：一条从最左贯到最右的灰线 + 均匀刻度点 + 白色圆点滑块。
 * 不用 Material3 的 Slider，是因为它两端有 inset、滑块两侧有 gap，怎么调都留缝。
 */
@Composable
private fun StepSlider(
    index: Int,
    count: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    var widthPx by remember { mutableStateOf(1f) }
    fun pick(x: Float): Int =
        (x / widthPx * (count - 1)).roundToInt().coerceIn(0, count - 1)

    Box(
        modifier = modifier
            .height(36.dp)
            .onGloballyPositioned { widthPx = it.size.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(count) {
                detectTapGestures { o ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    onChange((o.x / w * (count - 1)).roundToInt().coerceIn(0, count - 1))
                }
            }
            .pointerInput(count) {
                detectHorizontalDragGestures { change, _ ->
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    onChange((change.position.x / w * (count - 1)).roundToInt().coerceIn(0, count - 1))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            val cy = size.height / 2f
            val w = size.width
            drawLine(
                color = colors.outline,
                start = Offset(0f, cy),
                end = Offset(w, cy),
                strokeWidth = 16.dp.toPx(),
                cap = StrokeCap.Round,
            )
            val frac = if (count > 1) index.toFloat() / (count - 1) else 0f
            drawLine(
                color = colors.textTertiary,
                start = Offset(0f, cy),
                end = Offset(w * frac, cy),
                strokeWidth = 16.dp.toPx(),
                cap = StrokeCap.Round,
            )
            for (i in 0 until count) {
                val x = if (count > 1) w * i / (count - 1) else w / 2
                drawCircle(
                    color = if (i <= index) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.35f),
                    radius = 5.dp.toPx(),
                    center = Offset(x, cy),
                )
            }
        }
        val density = LocalDensity.current
        val frac = if (count > 1) index.toFloat() / (count - 1) else 0f
        val thumbX = with(density) { (widthPx * frac).toDp() }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbX - 8.dp)
                .size(16.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White),
        )
    }
}
