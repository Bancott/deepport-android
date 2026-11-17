package com.example.deepseekclone.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.deepseekclone.ui.theme.DsTheme
import kotlin.math.roundToInt

/**
 * 官方抽屉动画："推页式"——抽屉从左侧滑入，页面整体右移 82% 并轻微提亮，
 * 没有黑色遮罩。点击右移后仍可见的页面区域即关闭抽屉。
 */
@Composable
fun PushDrawerLayout(
    drawerOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    drawerWidthFraction: Float = 0.82f,
    drawer: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = DsTheme.colors
    val progress = remember { Animatable(if (drawerOpen) 1f else 0f) }

    LaunchedEffect(drawerOpen) {
        progress.animateTo(
            targetValue = if (drawerOpen) 1f else 0f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        val density = LocalDensity.current
        val drawerWidth = maxWidth * drawerWidthFraction
        val drawerWidthPx = with(density) { drawerWidth.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = drawerWidthPx * progress.value
                    if (progress.value > 0.01f) {
                        clip = true
                        shape = RoundedCornerShape(16.dp)
                    }
                },
        ) {
            content()
            if (progress.value > 0.01f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.05f * progress.value)),
                )
            }
        }

        if (progress.value > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset((drawerWidthPx * progress.value).roundToInt(), 0) }
                    .clickable(onClick = onDismiss),
            )
        }

        Box(
            modifier = Modifier
                .width(drawerWidth)
                .fillMaxHeight()
                .offset { IntOffset((-drawerWidthPx + drawerWidthPx * progress.value).roundToInt(), 0) }
                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
        ) {
            drawer(Modifier.fillMaxSize())
        }
    }
}
