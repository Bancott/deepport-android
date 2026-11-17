package com.example.deepseekclone.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.deepseekclone.R

/**
 * 官方几个"非 Material"图标，已按截图实测重绘为 VectorDrawable（res/drawable）：
 * - ic_menu_lines：上长下短两条方头线，18 / 10.3 × 1.8dp
 * - ic_plus_circle：Ø24.3 圈 + 方头十字
 * - ic_voice：Ø24.3 圈 + 圆点 + 两道声波弧（r 6 / 9.3）
 * - ic_deep_think：14dp 原子（外圈 + ±45° 双轨道 + 中心点）
 * - ic_new_chat：气泡 + 加号
 * 这里只做包装，保证 tint 与内容语义统一。
 */
@Composable
fun MenuGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_menu_lines),
        contentDescription = null,
        modifier = modifier.size(iconSize),
        tint = color,
    )
}

@Composable
fun PlusCircleGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 25.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_plus_circle),
        contentDescription = null,
        modifier = modifier.size(iconSize),
        tint = color,
    )
}

@Composable
fun VoiceGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 25.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_voice),
        contentDescription = null,
        modifier = modifier.size(iconSize),
        tint = color,
    )
}

@Composable
fun DeepThinkGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 14.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_deep_think),
        contentDescription = null,
        modifier = modifier.size(iconSize),
        tint = color,
    )
}

@Composable
fun NewChatGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 25.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_new_chat),
        contentDescription = null,
        modifier = modifier.size(iconSize),
        tint = color,
    )
}

/** 智能搜索：用户提供的互联网（地球）矢量，1024 viewBox，图形约占 858/1024 */
@Composable
fun GlobeGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 18.3.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_globe),
        contentDescription = null,
        modifier = modifier.size(iconSize),
        tint = color,
    )
}

/** 发送：向上箭头（Canvas 绘制，箭头比例与官方一致） */
@Composable
fun SendArrowGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 20.dp) {
    Canvas(modifier = modifier.size(iconSize)) {
        val s = size.minDimension
        val stroke = s * 0.11f
        val top = Offset(center.x, s * 0.20f)
        val bottom = Offset(center.x, s * 0.82f)
        drawLine(color, bottom, top, strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(
            color,
            Offset(center.x - s * 0.27f, s * 0.45f),
            top,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(center.x + s * 0.27f, s * 0.45f),
            top,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** 生成中：圆角方块（停止） */
@Composable
fun StopSquareGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 12.dp) {
    Canvas(modifier = modifier.size(iconSize)) {
        val s = size.minDimension
        drawRoundRect(
            color = color,
            size = Size(s, s),
            cornerRadius = CornerRadius(s * 0.26f, s * 0.26f),
        )
    }
}


/**
 * 上传/发送：与麦克风等大的描边圆 + 居中向上箭头（官方输入框有文字时就是这个按钮）
 * stroke 1.9dp、圆直径 24.6dp，箭头占圆内约 46%，比单纯箭头更醒目
 */
@Composable
fun SendCircleGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 24.6.dp) {
    Canvas(modifier = modifier.size(iconSize)) {
        val s = size.minDimension
        val stroke = s * 0.077f
        val r = s / 2f - stroke / 2f
        drawCircle(
            color = color,
            radius = r,
            style = Stroke(width = stroke),
        )
        // 箭头：杆 + 两撇，整体占圆直径的 ~46%
        val cx = center.x
        val topY = s * 0.30f
        val bottomY = s * 0.70f
        val headX = s * 0.115f
        val headY = s * 0.415f
        drawLine(color, Offset(cx, bottomY), Offset(cx, topY), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx - headX, headY), Offset(cx, topY), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(cx + headX, headY), Offset(cx, topY), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
