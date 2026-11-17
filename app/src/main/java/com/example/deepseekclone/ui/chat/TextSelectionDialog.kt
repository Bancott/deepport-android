package com.example.deepseekclone.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 「选择文本」：把整条消息放进弹窗里的 SelectionContainer，长按可自由选段；
 * 底部再给一个「全选并复制」的捷径。不直接给消息本体挂 SelectionContainer，
 * 是为了不抢长按菜单的手势。
 */
@Composable
fun TextSelectionDialog(
    text: String,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(colors.dialog),
        ) {
            Text(
                text = "选择文本",
                style = DsText.h2.copy(fontSize = 18.sp),
                color = colors.textPrimary,
                modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 10.dp),
            )
            SelectionContainer {
                Text(
                    text = text,
                    style = DsText.body.copy(fontSize = 15.sp, lineHeight = 24.sp),
                    color = colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                DialogAction(
                    text = "全选并复制",
                    color = colors.primary,
                    modifier = Modifier.weight(1f),
                ) {
                    copyToClipboard(context, text)
                    onDismiss()
                }
                DialogAction(
                    text = "关闭",
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun DialogAction(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = DsText.body.copy(fontSize = 16.sp), color = color)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}
