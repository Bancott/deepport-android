package io.github.bancott.deepport.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

/** 长按消息弹出的菜单项：图标 + 文案，和官方那种圆润浮层一致 */
private class MenuAction(
    val label: String,
    val onClick: () -> Unit,
    @androidx.annotation.DrawableRes val iconRes: Int,
)

/**
 * 消息长按菜单。
 * 官方 2.8 里有「喜欢 / 不喜欢 / 举报」，这三项对自建壳没有意义，已去掉。
 * 用户消息的「编辑」改成「修改并创建分支」（同一父节点下新建用户节点并重新作答）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageLongPressBox(
    message: MessageUi,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    /** false 时禁用长按手势（选择文本模式下交给文本自身），但不改变视图树结构 */
    enabled: Boolean = true,
    onRegenerate: () -> Unit = {},
    onEdit: () -> Unit = {},
    /** 传出手指长按点的窗口坐标，供「选择文本」定位被按的那一段 */
    onSelectText: (Offset) -> Unit = {},
    content: @Composable () -> Unit,
) {
    var pressWindow by remember { mutableStateOf(Offset.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box {
        Box(
            modifier = Modifier
                .onGloballyPositioned { origin = it.positionInWindow() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onTap = {},
                        onLongPress = { o ->
                            pressWindow = origin + o
                            onExpandedChange(true)
                        },
                    )
                },
        ) {
            content()
        }

        MessageMenuPopup(
            message = message,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            onRegenerate = onRegenerate,
            onEdit = onEdit,
            onSelectText = { onSelectText(pressWindow) },
        )
    }
}

/**
 * 只要菜单浮层、不要包裹手势的版本。
 * AI 回答那条是「外层 Column + 兄弟节点」结构，长按手势挂在外层，
 * 如果用包裹版会把 DropdownMenu 锚到一个 0 尺寸的盒子上（菜单弹到屏幕外看不见）。
 */
@Composable
fun MessageMenuPopup(
    message: MessageUi,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    anchorX: Dp = 0.dp,
    anchorY: Dp = 0.dp,
    onRegenerate: () -> Unit = {},
    onEdit: () -> Unit = {},
    /** 旧的原地选词回调：保留参数不动（lead 接完新面板后会删掉调用） */
    onSelectText: () -> Unit = {},
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
        add(
            MenuAction("复制", { copyToClipboard(); onExpandedChange(false) },
                iconRes = R.drawable.ic_copy_outline),
        )
        add(
            MenuAction("选择文本", {
                // 就地进入选择态：长按点所在的词/段直接预选，手柄、菜单都叠在这一条消息上，
                // 不再弹独立的「选择文字」面板。
                onSelectText()
                onExpandedChange(false)
            },
                iconRes = R.drawable.ic_select_outline),
        )
        if (isUser) {
            add(
                MenuAction("修改并创建分支", { onEdit(); onExpandedChange(false) },
                    iconRes = R.drawable.ic_edit_outline_20),
            )
        } else {
            add(
                MenuAction("重新生成", { onRegenerate(); onExpandedChange(false) },
                    iconRes = R.drawable.ic_refresh_outline_20),
            )
        }
        add(
            MenuAction("分享", { share(); onExpandedChange(false) },
                iconRes = R.drawable.ic_share_outline_20),
        )
    }

    // 菜单锚定在"长按点"：外层 Column 可能很高，如果直接挂在它上面会从左上角弹出
    Box(Modifier.offset(x = anchorX, y = anchorY)) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surface3,
        ) {
            actions.forEachIndexed { index, action ->
                // 复制/选择文本 与 重新生成/编辑/分享 之间加官方同款细分隔线
                if (index == 2) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 0.6.dp,
                        color = colors.outline,
                    )
                }
                DropdownMenuItem(
                    leadingIcon = {
                        SvgIcon(
                            action.iconRes,
                            tint = colors.textSecondary,
                            size = 19.dp,
                        )
                    },
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
