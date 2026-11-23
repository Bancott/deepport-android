package io.github.bancott.deepport.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
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
 * 顶栏「正在朗读」：以官方 ic_playing_fill_24 的喇叭本体为基础（闭合路径、实心填充），
 * 只让两道声波呼吸——1.05s 一轮，第二道滞后 160ms；透明度 0.35→1，并以弧线左端为轴横向 0.86→1.08。
 *
 * 本体直接用官方 drawable 的 pathData，保证和空闲态那枚喇叭是同一套轮廓；
 * 声波沿用消息动作条喇叭的弧线几何（半径 5 / 8.5、线宽 1.6），两处看起来才是一家人。
 */
@Composable
fun SpeakerWaveGlyph(
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    // 官方路径只在首次组合时解析一次
    val body = remember {
        PathParser.createPathFromPathData(SpeakerBodyPath)!!.asComposePath()
    }
    val clock = rememberInfiniteTransition(label = "speaker-wave")
    val time by clock.animateFloat(
        initialValue = 0f,
        targetValue = WavePeriodMs,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = WavePeriodMs.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave-clock",
    )

    Canvas(modifier.size(iconSize)) {
        val u = size.minDimension / 24f
        // 喇叭本体：闭合多边形，实心填充（不再描边）
        withTransform({ scale(u, u, pivot = Offset.Zero) }) {
            drawPath(body, color)
        }
        // 两道声波：尺寸与节奏都保持原样，只跟随主色
        val stroke = Stroke(width = 1.6f * u, cap = StrokeCap.Round)
        drawBreathingArc(time, delayMs = 0f, color, stroke, u, centerX = 11.93f, pivotX = 15.5f, radius = 5f)
        drawBreathingArc(time, delayMs = 160f, color, stroke, u, centerX = 11.98f, pivotX = 18f, radius = 8.5f)
    }
}

/** 官方 ic_playing_fill_24 的第一条路径：喇叭本体（24 画框，闭合填充） */
private const val SpeakerBodyPath =
    "M12.818,1.967L12.956,1.992L13.061,2.024C13.165,2.062 13.262,2.118 13.349,2.188C13.649,2.434 " +
        "13.694,2.764 13.705,2.856C13.718,2.96 13.716,3.077 13.716,3.123V20.888C13.716,20.929 " +
        "13.717,21.043 13.705,21.146C13.693,21.242 13.649,21.556 13.366,21.798C13.257,21.892 " +
        "13.127,21.961 12.988,22.001C12.63,22.102 12.344,21.965 12.258,21.921C12.165,21.875 " +
        "12.071,21.809 12.037,21.787L5.621,17.509H4.658C4.303,17.509 3.965,17.51 3.685,17.48C3.388,17.448 " +
        "3.052,17.372 2.741,17.146C2.58,17.029 2.438,16.887 2.321,16.726C2.096,16.416 2.019,16.08 " +
        "1.987,15.783C1.957,15.503 1.958,15.164 1.958,14.809V9.178C1.958,8.824 1.957,8.485 1.987,8.205C2.02,7.908 " +
        "2.096,7.572 2.321,7.261L2.414,7.144C2.511,7.031 2.62,6.928 2.741,6.84L2.859,6.763C3.135,6.597 " +
        "3.425,6.536 3.685,6.507C3.965,6.477 4.303,6.478 4.658,6.478H6.361L11.988,2.258C12.024,2.231 " +
        "12.117,2.16 12.208,2.107C12.279,2.066 12.51,1.937 12.818,1.967Z"

private const val WavePeriodMs = 1050f

/** 画一道会「呼吸」的声波：透明度与横向缩放都走同一条 ease-in-out 曲线，45% 处到峰值 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBreathingArc(
    timeMs: Float,
    delayMs: Float,
    color: Color,
    stroke: Stroke,
    unit: Float,
    centerX: Float,
    pivotX: Float,
    radius: Float,
) {
    val phase = (((timeMs - delayMs) % WavePeriodMs) + WavePeriodMs) % WavePeriodMs / WavePeriodMs
    val level = if (phase <= 0.45f) {
        FastOutSlowInEasing.transform(phase / 0.45f)
    } else {
        1f - FastOutSlowInEasing.transform((phase - 0.45f) / 0.55f)
    }
    val alpha = 0.35f + 0.65f * level
    val scaleX = 0.86f + 0.22f * level
    val arcSize = Size(radius * 2f * unit, radius * 2f * unit)
    val topLeft = Offset((centerX - radius) * unit, (12f - radius) * unit)
    withTransform({
        scale(scaleX, 1f, pivot = Offset(pivotX * unit, 12f * unit))
    }) {
        drawArc(
            color = color.copy(alpha = color.alpha * alpha),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke,
        )
    }
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



/**
 * 顶栏朗读开关：官方播放图标的「喇叭本体 + 声波」拆成两张 vector，
 * 关闭时声波淡出、斜线从左上往右下长出来；开启时反过来。
 *
 * 斜线几何按官方 ic_mute_audio_outline_24 的位图量取：24dp 画框 (7.2,7.2)→(18.4,19.2)、线宽 1.7dp、圆头。
 * 这样图标本身就是官方的矢量轮廓，不是另画一套。
 */
@Composable
fun SpeakerToggleGlyph(
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
) {
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (enabled) 0f else 1f,
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "speaker-slash",
    )
    Box(modifier = modifier.size(iconSize)) {
        SvgIcon(R.drawable.ic_speaker_body_24, tint = color, size = iconSize)
        SvgIcon(
            R.drawable.ic_speaker_waves_24,
            tint = color,
            size = iconSize,
            modifier = Modifier.alpha(1f - progress),
        )
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val unit = size.width / 24f
            val sx = 7.2f * unit
            val sy = 7.2f * unit
            val ex = 18.4f * unit
            val ey = 19.2f * unit
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(sx, sy),
                end = androidx.compose.ui.geometry.Offset(
                    sx + (ex - sx) * progress,
                    sy + (ey - sy) * progress,
                ),
                strokeWidth = 1.7f * unit,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
    }
}


