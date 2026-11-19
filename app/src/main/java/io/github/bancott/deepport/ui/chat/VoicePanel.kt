package io.github.bancott.deepport.ui.chat

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme
import kotlin.math.abs
import kotlin.math.sin

/*
 * 按住说话的全屏样式（对照官方录屏逐帧实测）：
 * - 底部一片蓝光：椭圆径向渐变，中心（0.5w, 0.98h）、半径（0.75w, 0.36h），
 *   峰值色 #176FF3，向上到约 62% 屏高处完全淡出；
 * - 提示文案居中，中心落在屏高 79.6% 处，"松手发送，上滑取消" 14sp 白色；
 * - 再往下 8% 是一排 28 根 2dp 小竖条（间距 4dp、总宽 164dp），中间高两边低；
 * - 上滑进入取消态：整片光变红、文案变 "松手取消"。
 */
private val BlueGlow = Color(0xFF176FF3)
private val RedGlow = Color(0xFFB3474D)
private val BlueBar = Color(0xFFFFFFFF)
private val RedBar = Color(0xFFD98080)

private const val HintCenterFraction = 0.796f
private const val WaveCenterFraction = 0.876f
private const val BarCount = 28

@Composable
fun VoiceOverlay(
    cancel: Boolean,
    levels: List<Float>,
    modifier: Modifier = Modifier,
) {
    val accent = if (cancel) RedGlow else BlueGlow
    val peak = if (cancel) 0.78f else 0.95f
    val barColor = if (cancel) RedBar else BlueBar
    val hintColor = if (cancel) DsTheme.colors.danger else Color.White

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenHeight = maxHeight

        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 1.05f)
            val radiusX = size.width * 1.15f
            val radiusY = size.height * 0.50f
            // 画布纵向压扁 → 圆形径向渐变变成椭圆，横向铺满、纵向收在底部
            withTransform({
                scale(scaleX = 1f, scaleY = radiusY / radiusX, pivot = center)
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to accent.copy(alpha = peak),
                            0.35f to accent.copy(alpha = peak * 0.90f),
                            0.55f to accent.copy(alpha = peak * 0.70f),
                            0.70f to accent.copy(alpha = peak * 0.32f),
                            0.86f to accent.copy(alpha = peak * 0.07f),
                            1.00f to Color.Transparent,
                        ),
                        center = center,
                        radius = radiusX,
                    ),
                    radius = radiusX,
                    center = center,
                )
            }
        }

        Text(
            text = if (cancel) "松手取消" else "松手发送，上滑取消",
            style = DsText.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = hintColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = screenHeight * HintCenterFraction - 10.dp),
        )

        Waveform(
            levels = levels,
            color = barColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = screenHeight * WaveCenterFraction - 8.dp),
        )
    }
}

/** 28 根 2dp 小竖条：中间高两边低，跟着最近电平起伏，静止时也有轻微呼吸 */
@Composable
private fun Waveform(
    levels: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
        ),
        label = "phase",
    )
    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(BarCount) { index ->
            val sample = levels.getOrNull(levels.size - BarCount + index) ?: 0f
            val shape = 0.55f + 0.45f * (1f - abs(index - BarCount / 2f) / (BarCount / 2f))
            val idle = 0.10f * sin(phase + index * 0.55f)
            val amplitude = (sample * shape + idle).coerceIn(0f, 1f)
            val barHeight = 3.dp + 11.dp * amplitude
            androidx.compose.foundation.layout.Box(
                Modifier
                    .width(2.dp)
                    .height(barHeight)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.9f)),
            )
        }
    }
}
