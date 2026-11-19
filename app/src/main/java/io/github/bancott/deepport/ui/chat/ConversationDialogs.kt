package io.github.bancott.deepport.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

/** 重命名对话：标题 + 圆角输入框 + 取消 / 确定 */
@Composable
fun RenameConversationDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    var value by remember {
        mutableStateOf(TextFieldValue(initial, selection = TextRange(0, initial.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.dialog),
        ) {
            Text(
                text = "重命名",
                style = DsText.h2.copy(fontSize = 19.sp),
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            BasicTextField(
                value = value,
                onValueChange = { if (it.text.length <= 60) value = it },
                singleLine = true,
                textStyle = DsText.body.copy(fontSize = 17.sp, color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.fieldBg)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (value.text.isEmpty()) {
                            Text("输入对话名称", style = DsText.body.copy(fontSize = 17.sp), color = colors.textTertiary)
                        }
                        inner()
                    }
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 8.dp),
            ) {
                DialogTextButton(
                    text = "取消",
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                DialogTextButton(
                    text = "确定",
                    color = colors.primary,
                    modifier = Modifier.weight(1f),
                    enabled = value.text.isNotBlank(),
                    onClick = {
                        onConfirm(value.text)
                        onDismiss()
                    },
                )
            }
        }
    }
}

/** 编辑用户消息：多行输入，确认后作为新分支重新回答 */
@Composable
fun EditMessageDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    var value by remember {
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.dialog),
        ) {
            Text(
                text = "编辑消息",
                style = DsText.h2.copy(fontSize = 19.sp),
                color = colors.textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            BasicTextField(
                value = value,
                onValueChange = { if (it.text.length <= 4000) value = it },
                textStyle = DsText.body.copy(fontSize = 16.sp, color = colors.textPrimary),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.fieldBg)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (value.text.isEmpty()) {
                            Text("输入消息", style = DsText.body.copy(fontSize = 16.sp), color = colors.textTertiary)
                        }
                        inner()
                    }
                },
            )
            Text(
                text = "发送后会作为新分支重新回答，原答案保留在 ‹n/m› 里",
                style = DsText.caption.copy(fontSize = 12.sp),
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            ) {
                DialogTextButton(
                    text = "取消",
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                DialogTextButton(
                    text = "发送",
                    color = colors.primary,
                    modifier = Modifier.weight(1f),
                    enabled = value.text.isNotBlank() && value.text != initial,
                    onClick = {
                        onConfirm(value.text)
                        onDismiss()
                    },
                )
            }
        }
    }
}

/** 删除确认（单个 / 多选共用） */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "删除",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.dialog),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = DsText.h2.copy(fontSize = 19.sp),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = message,
                    style = DsText.caption.copy(fontSize = 15.sp),
                    color = colors.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.outline),
            )
            Row(Modifier.fillMaxWidth().height(56.dp)) {
                DialogTextButton(
                    text = "取消",
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
                DialogTextButton(
                    text = confirmText,
                    color = colors.danger,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun DialogTextButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Box(
        modifier = modifier
            .height(56.dp)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = DsText.body.copy(fontSize = 17.sp),
            color = if (enabled) color else colors.textTertiary,
        )
    }
}
