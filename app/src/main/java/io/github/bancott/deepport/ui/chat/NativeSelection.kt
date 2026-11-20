package io.github.bancott.deepport.ui.chat

import android.widget.TextView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * App 内自绘的文本选择层（不依赖 TextView 的 textIsSelectable / 系统 ActionMode）。
 *
 * - 两个水滴手柄：圆形 + 下垂尖角，尖端对齐选区端点的行底；
 * - 拖动时把指针位置换算成字符偏移，实时改选区；
 * - 「复制 / 全选 / 朗读」浮层锚在选区上方，放不下就翻到下方；
 * - 点选区外或再点一下正文 → 退出选择。
 */
@Composable
internal fun NativeSelectionLayer(
    view: TextView,
    range: TextRange,
    origin: Offset,
    handleColor: Color,
    onRangeChange: (TextRange) -> Unit,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onSpeak: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf(0) } // 0 无 / 1 起点 / 2 终点
    val latestRange by rememberUpdatedState(range)

    val layout = view.layout ?: return
    val length = layout.text.length
    if (length == 0) return
    val start = range.start.coerceIn(0, length - 1)
    val end = range.end.coerceIn(start + 1, length)
    val startLine = layout.getLineForOffset(start)
    val endLine = layout.getLineForOffset(end - 1)
    val startPos = Offset(layout.getPrimaryHorizontal(start) + origin.x, layout.getLineBottom(startLine).toFloat() + origin.y)
    val endPos = Offset(layout.getPrimaryHorizontal(end) + origin.x, layout.getLineBottom(endLine).toFloat() + origin.y)

    fun offsetAt(pos: Offset): Int = view.getOffsetForPosition(pos.x - origin.x, pos.y - origin.y)

    Box(Modifier.fillMaxSize().onSizeChanged { boxSize = it }) {
        // 手势层：拖手柄 / 点空白退出
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(view) {
                    detectTapGestures(onTap = { onDismiss() })
                }
                .pointerInput(view) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            val ds = (pos - startPos).getDistance()
                            val de = (pos - endPos).getDistance()
                            dragging = when {
                                ds <= 40f && ds <= de -> 1
                                de <= 40f -> 2
                                else -> 0
                            }
                        },
                        onDrag = { change, _ ->
                            if (dragging == 0) return@detectDragGestures
                            change.consume()
                            val cur = latestRange
                            val off = offsetAt(change.position)
                            if (dragging == 1) {
                                val ns = off.coerceIn(0, cur.end - 1)
                                onRangeChange(TextRange(ns, cur.end))
                            } else {
                                val ne = off.coerceIn(cur.start + 1, length)
                                onRangeChange(TextRange(cur.start, ne))
                            }
                        },
                        onDragEnd = { dragging = 0 },
                        onDragCancel = { dragging = 0 },
                    )
                },
        )

        // 手柄（画在选区端点行底）
        Canvas(Modifier.matchParentSize()) {
            drawHandle(startPos, handleColor)
            drawHandle(endPos, handleColor)
        }

        // 浮层菜单：锚在选区上方（放不下翻到下面）
        val px = LocalDensity.current
        val menuWidthPx = if (menuSize.width > 0) menuSize.width.toFloat() else with(px) { 210.dp.toPx() }
        val menuHeightPx = if (menuSize.height > 0) menuSize.height.toFloat() else with(px) { 52.dp.toPx() }
        val midX = (startPos.x + endPos.x) / 2f
        val margin = with(px) { 8.dp.toPx() }
        val x = (midX - menuWidthPx / 2f).coerceIn(0f, (boxSize.width.toFloat() - menuWidthPx).coerceAtLeast(0f))
        val y = if (startPos.y - menuHeightPx - margin >= 0f) {
            startPos.y - menuHeightPx - margin
        } else {
            endPos.y + margin
        }
        Surface(
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .onSizeChanged { menuSize = it },
            shape = RoundedCornerShape(14.dp),
            color = colors.dialog,
            shadowElevation = 10.dp,
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SelectionAction(
                    label = "复制",
                    icon = { SvgIcon(io.github.bancott.deepport.R.drawable.ic_copy_outline, tint = colors.textPrimary, size = 17.dp) },
                    onClick = onCopy,
                )
                SelectionAction(
                    label = "全选",
                    icon = { SvgIcon(io.github.bancott.deepport.R.drawable.ic_select_all_outline_20, tint = colors.textPrimary, size = 18.dp) },
                    onClick = onSelectAll,
                )
                SelectionAction(
                    label = "朗读",
                    icon = { SvgIcon(io.github.bancott.deepport.R.drawable.ic_play_media_outline_20, tint = colors.textPrimary, size = 18.dp) },
                    onClick = onSpeak,
                )
            }
        }
    }
}

@Composable
private fun SelectionAction(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(4.dp))
        Text(label, style = DsText.caption.copy(fontSize = 13.sp), color = colors.textPrimary)
    }
}

/** 水滴手柄：圆形 + 下垂尖角，尖端在 (tip.x, tip.y) */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(tip: Offset, color: Color) {
    val r = 9.5.dp.toPx()
    val cx = tip.x
    val cy = tip.y - 16.dp.toPx()
    drawCircle(color, r, Offset(cx, cy))
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - r * 0.62f, cy + r * 0.62f)
        quadraticBezierTo(cx - r * 0.45f, tip.y - r * 0.9f, cx, tip.y)
        quadraticBezierTo(cx + r * 0.45f, tip.y - r * 0.9f, cx + r * 0.62f, cy + r * 0.62f)
        close()
    }
    drawPath(path, color)
}
