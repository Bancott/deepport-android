package io.github.bancott.deepport.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.bancott.deepport.ui.theme.DeepSeekTheme
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme
import kotlin.math.roundToInt

/** 角色定义长度上限：够写一段人格/规则，又不至于把 system 撑爆 */
private const val ROLE_MAX_LENGTH = 2000

/**
 * 「个性化 → 角色定义」编辑弹窗，视觉沿用设置页的深灰圆角卡片 + 底部双按钮。
 */
@Composable
fun RoleDefinitionDialog(
    initial: String,
    darkTheme: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember {
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        DeepSeekTheme(darkTheme = darkTheme) {
            val colors = DsTheme.colors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.dialog),
            ) {
                Text(
                    text = "角色定义",
                    style = DsText.h2.copy(fontSize = 19.sp),
                    color = colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 14.dp),
                    textAlign = TextAlign.Center,
                )
                BasicTextField(
                    value = value,
                    onValueChange = {
                        if (it.text.length <= ROLE_MAX_LENGTH) value = it
                    },
                    textStyle = DsText.body.copy(fontSize = 16.sp, color = colors.textPrimary),
                    cursorBrush = SolidColor(colors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .heightIn(min = 132.dp, max = 240.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.fieldBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .focusRequester(focus),
                    decorationBox = { inner ->
                        if (value.text.isEmpty()) {
                            Text(
                                text = "例如：你是一名严谨的技术编辑，回答先给结论，再给要点；不要使用夸张语气。",
                                style = DsText.body.copy(fontSize = 16.sp),
                                color = colors.textTertiary,
                            )
                        }
                        inner()
                    },
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "会附加在系统提示末尾",
                        style = DsText.caption.copy(fontSize = 12.sp),
                        color = colors.textTertiary,
                    )
                    Text(
                        text = "${value.text.length}/$ROLE_MAX_LENGTH",
                        style = DsText.caption.copy(fontSize = 12.sp),
                        color = colors.textTertiary,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) {
                    DialogTextButton(
                        text = "取消",
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    DialogTextButton(
                        text = "保存",
                        color = colors.primary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onConfirm(value.text.trim())
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

/**
 * 「个性化 → 温度」调节弹窗：0–2 步进 0.1，带档位描述。
 */
@Composable
fun TemperatureDialog(
    initial: Float,
    darkTheme: Boolean,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableFloatStateOf(initial.coerceIn(0f, 2f)) }
    Dialog(onDismissRequest = onDismiss) {
        DeepSeekTheme(darkTheme = darkTheme) {
            val colors = DsTheme.colors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.dialog),
            ) {
                Text(
                    text = "温度",
                    style = DsText.h2.copy(fontSize = 19.sp),
                    color = colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = String.format(java.util.Locale.CHINA, "%.1f", draft),
                    style = DsText.h2.copy(fontSize = 34.sp),
                    color = colors.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = temperatureHint(draft),
                    style = DsText.caption.copy(fontSize = 13.sp),
                    color = colors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    textAlign = TextAlign.Center,
                )
                Slider(
                    value = draft,
                    onValueChange = { draft = (it * 10f).roundToInt() / 10f },
                    valueRange = 0f..2f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primary,
                        activeTrackColor = colors.primary,
                        inactiveTrackColor = colors.surface3,
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("精确", style = DsText.caption.copy(fontSize = 12.sp), color = colors.textTertiary)
                    Text("发散", style = DsText.caption.copy(fontSize = 12.sp), color = colors.textTertiary)
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    DialogTextButton(
                        text = "取消",
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                    )
                    DialogTextButton(
                        text = "保存",
                        color = colors.primary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onConfirm(draft)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

private fun temperatureHint(value: Float): String = when {
    value <= 0.3f -> "更稳定、更聚焦，适合事实与代码"
    value <= 0.8f -> "偏严谨，适合日常问答"
    value <= 1.2f -> "平衡（推荐）"
    value <= 1.6f -> "更发散，适合创意写作"
    else -> "很发散，可能出现非常规表达"
}

/** 与设置页弹窗按钮同款：透明底、主色/次级色文字、按压波纹 */
@Composable
private fun DialogTextButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = DsText.button.copy(fontSize = 16.sp),
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
