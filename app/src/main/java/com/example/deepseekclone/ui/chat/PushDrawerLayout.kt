package com.example.deepseekclone.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.deepseekclone.ui.theme.DsTheme
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 官方抽屉动画（推页式）。层级关系：**抽屉在最底，会话页作为卡片浮在上面**。
 *
 * 打开时：
 * - 抽屉从左侧滑入（-W → 0），跟手 1:1；
 * - 会话页右移、缩小到 0.93、内容淡到 38%，四角切 20dp 圆角，左缘一条 1dp 高光；
 * - 页面位移用 p^2.2，比抽屉"慢半拍"，所以抽屉看起来是从页面身后露出来的。
 *
 * 手势：页面任意位置右滑打开、左滑关闭、点击露出的页面区域关闭。
 */
@Composable
fun PushDrawerLayout(
    drawerOpen: Boolean,
    onDrawerOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    drawerWidthFraction: Float = 0.82f,
    drawer: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = DsTheme.colors
    val progress = remember { Animatable(if (drawerOpen) 1f else 0f) }
    val scope = rememberCoroutineScope()

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

        fun settle(open: Boolean) {
            onDrawerOpenChange(open)
            scope.launch {
                progress.animateTo(
                    targetValue = if (open) 1f else 0f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawerOpen, drawerWidthPx) {
                    var dragging = false
                    detectHorizontalDragGestures(
                        onDragStart = { dragging = true },
                        onHorizontalDrag = { change, dragAmount ->
                            if (dragging) {
                                change.consume()
                                scope.launch {
                                    progress.snapTo(
                                        (progress.value + dragAmount / drawerWidthPx).coerceIn(0f, 1f),
                                    )
                                }
                            }
                        },
                        onDragEnd = {
                            if (dragging) {
                                dragging = false
                                settle(progress.value > 0.5f)
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            settle(progress.value > 0.5f)
                        },
                    )
                },
        ) {
            /* ---------- 1. 抽屉（最底层） ---------- */
            Box(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .offset {
                        IntOffset((-drawerWidthPx + drawerWidthPx * progress.value).roundToInt(), 0)
                    },
            ) {
                drawer(Modifier.fillMaxSize())
            }

            /* ---------- 2. 会话页（上层卡片，随推页右移 + 缩小 + 变淡） ---------- */
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageP = progress.value.pow(2.2f)
                        translationX = drawerWidthPx * pageP
                        val s = 1f - 0.07f * pageP
                        scaleX = s
                        scaleY = s
                        if (progress.value > 0.01f) {
                            clip = true
                            shape = RoundedCornerShape(20.dp)
                        }
                    }
                    .background(colors.pushedPage),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageP = progress.value.pow(2.2f)
                            alpha = 1f - 0.62f * pageP
                        },
                ) {
                    content()
                }

                // 卡片左缘 1dp 高光（官方截图 ≈ #2B2B2B），强化"浮起的一层"
                if (progress.value > 0.01f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.07f * progress.value)),
                    )
                }
            }

            /* ---------- 3. 点击露出的页面区域关闭 ---------- */
            if (progress.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((drawerWidthPx * progress.value).roundToInt(), 0) }
                        .clickable { onDrawerOpenChange(false) },
                )
            }
        }
    }
}
