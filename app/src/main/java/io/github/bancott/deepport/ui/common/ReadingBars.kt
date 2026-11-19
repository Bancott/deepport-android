package io.github.bancott.deepport.ui.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.bancott.deepport.ui.chat.ReadAloudState

/**
 * 朗读按钮里的四根竖条（对应官方 chat-demo 的 ReadingBars）：
 * Loading 是四根短条呼吸，Playing 从左到右波动（相邻条相位差 90ms）。
 * 仅做装饰，语义由外层按钮的 contentDescription 承担。
 */
@Composable
fun ReadingBars(
    state: ReadAloudState,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val playing = state == ReadAloudState.Playing
    val transition = rememberInfiniteTransition(label = "reading-bars")
    val waves = List(4) { index ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 700,
                    delayMillis = index * 90,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar-$index",
        )
    }

    Row(
        modifier = modifier
            .width(16.dp)
            .height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        waves.forEach { wave ->
            val height = if (playing) (4f + 10f * wave.value).dp else 4.dp
            val alpha = if (playing) 1f else 0.4f + 0.6f * wave.value
            Box(
                Modifier
                    .width(2.dp)
                    .height(height)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color.copy(alpha = alpha)),
            )
        }
    }
}
