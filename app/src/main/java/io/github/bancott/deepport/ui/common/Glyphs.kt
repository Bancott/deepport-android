package io.github.bancott.deepport.ui.common

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
import io.github.bancott.deepport.R

/**
 * 官方 2.8.0 组件图标（`res/drawable/ic_*`，多数由 APK 直接提取）。
 *
 * 早期版本用 chat-demo 的 Web SVG 和截图描边；现在以 Android APK 里的
 * VectorDrawable / 4x PNG 为准，路径、线宽、光学留白都跟官方一致。
 * 这里只做尺寸与 tint 包装，不再二次改几何。
 */
@Composable
fun MenuGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    SvgIcon(R.drawable.ic_menu_outline_24, tint = color, modifier = modifier, size = iconSize)
}

@Composable
fun PlusCircleGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 26.dp) {
    SvgIcon(R.drawable.ic_add_outline_26, tint = color, modifier = modifier, size = iconSize)
}

@Composable
fun VoiceGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 26.dp) {
    SvgIcon(R.drawable.ic_voice_outline_26, tint = color, modifier = modifier, size = iconSize)
}

@Composable
fun KeyboardGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 26.dp) {
    SvgIcon(R.drawable.ic_keyboard_outline_26, tint = color, modifier = modifier, size = iconSize)
}

@Composable
fun NewChatGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 24.dp) {
    SvgIcon(R.drawable.ic_new_chat_outline_24, tint = color, modifier = modifier, size = iconSize)
}

@Composable
fun DeepThinkGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 15.dp) {
    SvgIcon(R.drawable.ic_think_outline_16, tint = color, modifier = modifier, size = iconSize)
}

/** 智能搜索：官方 16dp 地球，图形占满 16dp 画框 */
@Composable
fun GlobeGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 16.dp) {
    SvgIcon(R.drawable.ic_globe_language_outline_16, tint = color, modifier = modifier, size = iconSize)
}

/** 发送：官方 16dp 实心向上箭头 */
@Composable
fun SendArrowGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 14.dp) {
    SvgIcon(R.drawable.ic_send_fill_20, tint = color, modifier = modifier, size = iconSize)
}

/** 生成中：官方 16dp 圆角方块（停止） */
@Composable
fun StopSquareGlyph(color: Color, modifier: Modifier = Modifier, iconSize: Dp = 16.dp) {
    SvgIcon(R.drawable.ic_stop_fill_20, tint = color, modifier = modifier, size = iconSize)
}

/**
 * 顶栏自动朗读：直接使用官方 2.8 的 24dp 三态图标。
 * - 关闭：mute_audio_outline_24（原生带斜线，不再手绘）
 * - 开启但未播放：play_media_outline_24
 * - 正在播放：playing_fill_24
 */
@Composable
fun SpeakerGlyph(
    color: Color,
    muted: Boolean,
    reading: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    val res = when {
        reading -> R.drawable.ic_playing_fill_24
        muted -> R.drawable.ic_mute_audio_outline_24
        else -> R.drawable.ic_play_media_outline_24
    }
    SvgIcon(res, tint = color, modifier = modifier, size = iconSize)
}

/**
 * 发送：蓝色实心圆 + 官方 16dp 白色向上箭头（有文字时替换语音按钮，外径与麦克风一致 24.6dp）
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
        // 箭头随圆钮等比缩放（24.6dp 圆配 12.5dp 箭头，官方 31dp 圆约配 15.8dp）
        SvgIcon(R.drawable.ic_send_fill_20, tint = arrowColor, size = iconSize * 0.51f)
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


