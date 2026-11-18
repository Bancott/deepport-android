package com.example.deepseekclone.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.deepseekclone.R

/**
 * 图标分两类：
 * - 官方 chat-demo `icons.tsx` 里的 SVG path 原样转成的 VectorDrawable（菜单 / 深度思考 / 地球 /
 *   新对话 / 复制 / 重新生成 / 朗读 / 发送 / 停止），见 `_audit/import_icons.py`；
 * - 官方安卓 App 截图里才有、chat-demo 没有的（语音输入的圆环声波、分享箭头、加号圈、键盘、鲸鱼），
 *   继续用按 3x 截图描的矢量。
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


/** 智能搜索：用户提供的互联网（地球）矢量，1024 viewBox，图形约占 858/1024 */
@Composable
fun GlobeGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 15.7.dp) {
    Icon(
        painter = painterResource(R.drawable.ic_globe),
        contentDescription = null,
        modifier = modifier.size(iconSize),
        tint = color,
    )
}

/** 发送：官方 chat-demo 的向上箭头 */
@Composable
fun SendArrowGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 20.dp) {
    SvgIcon(R.drawable.ic_send, tint = color, modifier = modifier, size = iconSize)
}

/** 生成中：官方 chat-demo 的圆角方块（停止） */
@Composable
fun StopSquareGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 12.dp) {
    SvgIcon(R.drawable.ic_stop, tint = color, modifier = modifier, size = iconSize)
}


/**
 * 发送：蓝色实心圆 + 官方 chat-demo 的白色向上箭头（有文字时替换语音按钮，尺寸与麦克风一致 24.6dp）
 */
@Composable
fun SendCircleGlyph(
    background: Color,
    arrowColor: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.6.dp,
) {
    Box(modifier = modifier.size(iconSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(color = background, radius = size.minDimension / 2f)
        }
        SvgIcon(R.drawable.ic_send, tint = arrowColor, size = iconSize * 0.46f)
    }
}

/** 用用户提供的 SVG（已转 VectorDrawable）渲染，统一 22dp 画框并按比例缩放 */
@Composable
fun SvgIcon(
    @androidx.annotation.DrawableRes resId: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}


