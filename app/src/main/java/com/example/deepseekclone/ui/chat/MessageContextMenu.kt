package com.example.deepseekclone.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/** 长按消息弹出的菜单项 */
private data class MenuAction(val label: String, val onClick: () -> Unit)

/**
 * 消息长按菜单（官方 2.8 的 6 项：复制 / 选择文本 / 重新生成 / 朗读 / 分享 / 举报；
 * 用户消息额外有"编辑"）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageLongPressBox(
    message: MessageUi,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onRegenerate: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = DsTheme.colors

    fun copyToClipboard() {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("message", message.text))
    }

    fun share() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message.text)
        }
        context.startActivity(Intent.createChooser(intent, "分享"))
    }

    val isUser = message.role == ChatRole.User
    val actions = buildList {
        add(MenuAction("复制") { copyToClipboard(); onExpandedChange(false) })
        add(MenuAction("选择文本") { onExpandedChange(false) })
        if (isUser) {
            add(MenuAction("编辑") { onExpandedChange(false) })
        } else {
            add(MenuAction("重新生成") { onRegenerate(); onExpandedChange(false) })
            add(MenuAction("朗读") { onExpandedChange(false) })
        }
        add(MenuAction("分享") { share(); onExpandedChange(false) })
        add(MenuAction("举报") { onExpandedChange(false) })
    }

    Box {
        Box(
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { onExpandedChange(true) },
            ),
        ) {
            content()
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = action.label,
                            style = DsText.label.copy(fontSize = 15.sp),
                            color = colors.textPrimary,
                        )
                    },
                    onClick = action.onClick,
                )
            }
        }
    }
}
