package com.example.deepseekclone.ui.chat

import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.widget.TextViewCompat
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.SvgIcon
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/** 窗口宽度到这条线就走宽屏 Dialog（官方 ChatMessageTextSelectionSheet.kt:66 同款分流） */
private const val WIDE_SCREEN_WIDTH_DP = 600

/**
 * 官方「选择文字」模态面板（APK 2.8.0 的 ChatMessageTextSelectionSheet.kt）。
 *
 * 和长按菜单里的「选择文本」配套：点菜单不再原地选词，而是把这条消息的**纯文本**
 * 放进一个模态面板，由用户自己长按/拖动去选。
 *
 * - 手机（窗口 < 600dp）用 ModalBottomSheet，宽屏用 Dialog；
 * - 标题「选择文字」，右上角 40dp 关闭圆钮，下滑也可关闭（sheet）；
 * - 正文是原生 TextView（textIsSelectable），系统 ActionMode / 系统选区手柄，**不预选**、不自绘；
 * - 正文 17sp / 行高 28sp，内边距 start 24 / top 4 / end 24 / bottom 72dp（再叠 navigation bar inset）。
 *
 * 宿主只需要：菜单点击时把 `message.text` 记成状态并打开本面板，`onDismiss` 里清掉状态。
 * 聊天页的顶栏/底栏不用动，模态会盖在上面。
 */
@Composable
fun MessageTextSelectionSheet(
    text: String,
    onDismiss: () -> Unit,
) {
    if (LocalConfiguration.current.screenWidthDp >= WIDE_SCREEN_WIDTH_DP) {
        TextSelectionDialog(content = text, onDismiss = onDismiss)
    } else {
        TextSelectionBottomSheet(content = text, onDismiss = onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextSelectionBottomSheet(
    content: String,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface2,
        dragHandle = null,
        // navigation bar inset 由正文自己补（见 SelectableMessageText），这里别再垫一层，避免重复
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
        ) {
            TextSelectionTitleBar(onClose = onDismiss)
            SelectableMessageText(
                content = content,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun TextSelectionDialog(
    content: String,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 宽屏下别把卡片拉满：宽封顶 620dp，高最多 720dp
            val cardWidth = minOf(maxWidth, 620.dp)
            val cardHeight = minOf(maxHeight * 0.9f, 720.dp)
            Column(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.dialog),
            ) {
                TextSelectionTitleBar(onClose = onDismiss)
                SelectableMessageText(
                    content = content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

/** 面板顶栏：居中「选择文字」+ 右上角关闭，底部一条 1dp 分隔线（官方 TitleBar 同款结构） */
@Composable
private fun TextSelectionTitleBar(onClose: () -> Unit) {
    val colors = DsTheme.colors

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = "选择文字",
                style = DsText.titleBar,
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
            CircleIconButton(
                contentDescription = "关闭",
                onClick = onClose,
                containerColor = colors.fieldBg,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
            ) {
                SvgIcon(
                    R.drawable.ic_close_outline_20,
                    tint = colors.textSecondary,
                    size = 18.dp,
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline),
        )
    }
}

/**
 * 正文：官方同款 AndroidView + 原生 TextView。
 * 打开时把整条文本 setText 进去（不 setSelection），交给系统 ActionMode / 手柄；
 * 文本没变时不重复 setText，否则用户正在拖的选区会被清掉。
 */
@Composable
private fun SelectableMessageText(
    content: String,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    val density = LocalDensity.current

    // 官方：正文色 = 正文色，highlight = 主题选区色（全局 TextSelectionColors 的 backgroundColor，即主色 40%）
    val textColor = colors.textPrimary.toArgb()
    val highlightColor = colors.primary.copy(alpha = 0.4f).toArgb()
    val scrollbarColor = colors.textSecondary.copy(alpha = 0.2f).toArgb()

    // 官方 SelectableTextView：padding 24 / 4 / 24 / (72 + navigation bar inset)，单位原生 px
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val horizontalPx = with(density) { 24.dp.roundToPx() }
    val topPx = with(density) { 4.dp.roundToPx() }
    val bottomPx = with(density) { (72.dp + navBarBottom).roundToPx() }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val textView = TextView(context)
            textView.setTextIsSelectable(true)
            textView.includeFontPadding = false
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            TextViewCompat.setLineHeight(textView, TypedValue.COMPLEX_UNIT_SP, 28f)
            textView.letterSpacing = 0f
            textView.setTextColor(textColor)
            textView.setHighlightColor(highlightColor)
            textView.setPaddingRelative(horizontalPx, topPx, horizontalPx, bottomPx)
            textView.text = content

            ScrollView(context).apply {
                scrollBarStyle = View.SCROLLBARS_INSIDE_INSET
                setClipToPadding(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    verticalScrollbarThumbDrawable?.setTint(scrollbarColor)
                }
                addView(
                    textView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }
        },
        update = { scrollView ->
            val textView = scrollView.getChildAt(0) as? TextView
            if (textView != null) {
                // 同样的文本不要重复 setText：那会清掉用户当前的选择
                if (textView.text?.toString() != content) {
                    textView.text = content
                }
                textView.setTextColor(textColor)
                textView.setHighlightColor(highlightColor)
                textView.setPaddingRelative(horizontalPx, topPx, horizontalPx, bottomPx)
            }
        },
    )
}
