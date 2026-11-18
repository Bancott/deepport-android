package com.example.deepseekclone.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import com.example.deepseekclone.ui.theme.DsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.pow

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
    drawerWidthFraction: Float = 0.825f,
    drawer: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = DsTheme.colors
    val isLight = colors.bg.luminance() > 0.5f
    // 直接以目标态起手：从设置页返回时，会话页那层仍记着 drawerOpen=true，
    // 若从 0 起播动画会先闪一帧纯会话界面再"拉回"抽屉，反而不符合预期。
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
            // 卡片外侧露出的必须是历史界面的颜色，不能是第三种底色
            .background(colors.drawerSurface),
    ) {
        val density = LocalDensity.current
        val drawerWidth = maxWidth * drawerWidthFraction
        val drawerWidthPx = with(density) { drawerWidth.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val dragSpanPx = screenWidthPx * DRAG_SPAN_RATIO
        // 每帧都要用的量提前算好，别把 dp→px 的密度查找带进绘制阶段
        val pageTravelPx = with(density) { (drawerWidth - 10.dp).toPx() }
        val liftedShape = remember { RoundedCornerShape(20.dp) }

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
                    // 用 graphicsLayer 平移：只走绘制阶段，抽屉里的 LazyColumn 不会每帧重新布局；
                    // 也不取整，浮点位移才不会有整像素跳格（之前字体发颤的主因之一）
                    .graphicsLayer { translationX = -drawerWidthPx + drawerWidthPx * progress },
            ) {
                drawer(Modifier.fillMaxSize())

            }

            /* ---------- 2. 会话页（上层卡片） ---------- */
            // 不做投影：卡片外侧露出的就是历史界面本身，保证全屏只有两种颜色。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val pageP = progress.pow(LAG_POWER)
                        translationX = pageTravelPx * pageP
                        val s = 1f - 0.052f * pageP
                        scaleX = s
                        scaleY = s
                        clip = pageLifted.value
                        if (pageLifted.value) shape = liftedShape
                    }
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
                if (pageLifted.value) {
                    // 颜色固定、亮度交给 graphicsLayer：避免在组合阶段读 progress 每帧重组这个 Box。
                    // graphicsLayer 必须写在 background 之前（外层），否则 alpha 罩不住背景，
                    // 会得到一条 100% 不透明的纯白竖线。
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(1.dp)
                            .graphicsLayer { alpha = (if (isLight) 0.06f else 0.07f) * progress }
                            .background(if (isLight) Color.Black else Color.White),
                    )
                }
            }

            /* ---------- 3. 点击露出的页面区域关闭 ---------- */
            if (pageLifted.value) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 只做绘制平移，避免每帧全屏重新布局；
                        // graphicsLayer 的矩阵会参与坐标换算，点击热区依旧跟着卡片走
                        .graphicsLayer { translationX = drawerWidthPx * progress }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDrawerOpenChange(false) },
                )
            }
        }
    }
}
