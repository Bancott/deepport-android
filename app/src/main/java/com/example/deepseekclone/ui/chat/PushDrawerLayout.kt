package com.example.deepseekclone.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.deepseekclone.ui.theme.DsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

private const val LAG_POWER = 2.2f

/** 手指位移 → 露出比例的换算长度：约 200dp 行程即可全开 */
private const val DRAG_SPAN_RATIO = 0.5f

/** 快速甩动的速度阈值（px/s），超过则不看距离直接朝该方向吸附 */
private const val FLING_VELOCITY = 700f

/**
 * 官方抽屉动画（推页式）。层级：**抽屉在底层，会话页作为卡片浮在上面**。
 *
 * - 抽屉从左侧滑入（-W → 0）；
 * - 会话页右移、缩小到 0.93、内容淡到 38%、20dp 圆角、左缘 1dp 高光；
 * - 页面位移用 p^2.2（比抽屉慢半拍），抽屉像是从页面身后露出来；
 * - 松手吸附：**速度优先**（快速甩动按方向切换），否则看谁占的面积大。
 */
@Composable
fun PushDrawerLayout(
    drawerOpen: Boolean,
    onDrawerOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    drawerWidthFraction: Float = 0.80f,
    drawer: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = DsTheme.colors
    val isLight = colors.bg.luminance() > 0.5f
    var progress by remember { mutableFloatStateOf(if (drawerOpen) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }
    var animJob by remember { mutableStateOf<Job?>(null) }
    // 只在跨越阈值时重组一次，避免动画期间每帧重组整个会话页（卡顿根因）
    val pageLifted = remember { derivedStateOf { progress > 0.01f } }

    fun animateProgressTo(target: Float, duration: Int = 280) {
        animJob?.cancel()
        animJob = scope.launch {
            animate(
                initialValue = progress,
                targetValue = target,
                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
            ) { value, _ -> progress = value }
        }
    }

    // 外部状态变化（点菜单按钮）时补动画
    LaunchedEffect(drawerOpen) {
        animateProgressTo(if (drawerOpen) 1f else 0f)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        val density = LocalDensity.current
        val drawerWidth = maxWidth * drawerWidthFraction
        val drawerWidthPx = with(density) { drawerWidth.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val dragSpanPx = screenWidthPx * DRAG_SPAN_RATIO

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawerOpen, dragSpanPx) {
                    var dragging = false

                    fun settle(open: Boolean) {
                        val stateChanged = drawerOpen != open
                        onDrawerOpenChange(open)
                        if (!stateChanged) {
                            // 状态没变时 LaunchedEffect 不会触发，这里手动收尾
                            animateProgressTo(if (open) 1f else 0f, duration = 240)
                        }
                    }

                    fun shouldOpen(): Boolean {
                        val vx = velocityTracker.calculateVelocity().x
                        return when {
                            vx > FLING_VELOCITY -> true
                            vx < -FLING_VELOCITY -> false
                            else -> progress.pow(LAG_POWER) > 0.5f
                        }
                    }

                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            animJob?.cancel()
                            velocityTracker.resetTracking()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (dragging) {
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                // 手指位移按增益映射到"露出比例"，再反推动画进度
                                val revealed = progress.pow(LAG_POWER) + dragAmount / dragSpanPx
                                progress = revealed.coerceIn(0f, 1f).pow(1f / LAG_POWER)
                            }
                        },
                        onDragEnd = {
                            if (dragging) {
                                dragging = false
                                settle(shouldOpen())
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            settle(progress.pow(LAG_POWER) > 0.5f)
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
                        IntOffset((-drawerWidthPx + drawerWidthPx * progress).roundToInt(), 0)
                    },
            ) {
                drawer(Modifier.fillMaxSize())

                // 浅色模式：官方在抽屉右缘有约 24dp 的柔和投影（#FCFCFC → #F0F0F0），
                // 深色模式没有，所以只在这里画。
                if (isLight) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(30.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.035f)),
                                ),
                            ),
                    )
                }
            }

            /* ---------- 2. 会话页（上层卡片） ---------- */
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageP = progress.pow(LAG_POWER)
                        translationX = drawerWidthPx * pageP
                        val s = 1f - 0.052f * pageP
                        scaleX = s
                        scaleY = s
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp),
                        clip = false,
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.pushedPage),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val pageP = progress.pow(LAG_POWER)
                            alpha = if (isLight) 1f - 0.75f * pageP else 1f - 0.62f * pageP
                        },
                ) {
                    content()
                }

                // 卡片左缘 1dp 高光（官方 ≈ #2B2B2B）
                if (!isLight && pageLifted.value) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(Color.White.copy(alpha = 0.07f * progress)),
                    )
                }
            }

            /* ---------- 3. 点击露出的页面区域关闭 ---------- */
            if (pageLifted.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((drawerWidthPx * progress).roundToInt(), 0) }
                        .clickable { onDrawerOpenChange(false) },
                )
            }
        }
    }
}










