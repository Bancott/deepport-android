package io.github.bancott.deepport.ui.chat

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.bancott.deepport.ui.theme.DsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 快速甩动的速度阈值（px/s），超过则不看距离直接朝该方向吸附 */
private const val FLING_VELOCITY = 700f

/**
 * 官方抽屉动画（推页式）。层级：**抽屉在底层，会话页作为卡片浮在上面**。
 *
 * - 拖拽 **1:1 线性跟手**（Material3 AnchoredDraggable 语义），页面与抽屉同步移动、无滞后；
 * - 抽屉从左侧滑入（-W → 0）；
 * - 会话页右移、缩小到 0.95、内容洗淡到 45%（深）/25%（浅）、20dp 圆角、左缘 1dp 高光；
 * - 抽屉面在滑出过程中压一层 (1−p)×0.2 黑纱，全开时褪尽（官方 ij7 case2 的 ContentScrim）；
 * - 松手吸附：spring(damping 0.9, stiffness 1500)（官方 mj7.a），**速度优先**（快速甩动按方向切换），
 *   否则看进度是否过半。
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

    fun animateProgressTo(target: Float) {
        animJob?.cancel()
        animJob = scope.launch {
            // 官方收尾是弹簧物理（spring damping 0.9 / stiffness 1500，mj7.a），没有固定毫秒数
            animate(
                initialValue = progress,
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 1500f),
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
        // 每帧都要用的量提前算好，别把 dp→px 的密度查找带进绘制阶段
        val pageTravelPx = with(density) { (drawerWidth - 10.dp).toPx() }
        val liftedShape = remember { RoundedCornerShape(20.dp) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawerOpen, pageTravelPx) {
                    val edgePx = 28.dp.toPx()
                    var dragging = false
                    var startX = 0f
                    var totalX = 0f
                    var totalY = 0f

                    fun settle(open: Boolean) {
                        val stateChanged = drawerOpen != open
                        onDrawerOpenChange(open)
                        if (!stateChanged) {
                            // 状态没变时 LaunchedEffect 不会触发，这里手动收尾
                            animateProgressTo(if (open) 1f else 0f)
                        }
                    }

                    fun shouldOpen(): Boolean {
                        val vx = velocityTracker.calculateVelocity().x
                        return when {
                            vx > FLING_VELOCITY -> true
                            vx < -FLING_VELOCITY -> false
                            else -> progress > 0.5f
                        }
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        startX = down.position.x
                        // 关闭状态下只允许从左边缘 28dp 内起手，别抢消息区（WebView / 代码块横滑）的手势
                        if (!drawerOpen && startX > edgePx) {
                            return@awaitEachGesture
                        }
                        dragging = false
                        totalX = 0f
                        totalY = 0f
                        var finished = false
                        while (!finished) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUp()) {
                                finished = true
                                continue
                            }
                            val dx = change.position.x - change.previousPosition.x
                            val dy = change.position.y - change.previousPosition.y
                            totalX += dx
                            totalY += dy
                            if (!dragging &&
                                kotlin.math.abs(totalX) > viewConfiguration.touchSlop &&
                                kotlin.math.abs(totalX) > kotlin.math.abs(totalY)
                            ) {
                                dragging = true
                                animJob?.cancel()
                                velocityTracker.resetTracking()
                            }
                            if (dragging) {
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                progress = (progress + dx / pageTravelPx).coerceIn(0f, 1f)
                            }
                        }
                        if (dragging) settle(shouldOpen())
                        dragging = false
                    }
                },
            ) {
                drawer(Modifier.fillMaxSize())

                // 抽屉面黑纱：滑出过程中压 (1−p)×0.2，全开时褪尽（官方 ContentScrim 的黑罩，
                // 颜色固定、alpha 交给 graphicsLayer，避免每帧重组）
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = (1f - progress) * 0.2f }
                        .background(Color.Black),
                )
            }

            /* ---------- 2. 会话页（上层卡片） ---------- */
            // 不做投影：卡片外侧露出的就是历史界面本身，保证全屏只有两种颜色。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // 线性 1:1：页面与手指同速，不再做幂滞后
                        translationX = pageTravelPx * progress
                        val s = 1f - 0.05f * progress
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
                            // 官方内容洗淡：深色收尾 ≈0.45、浅色 ≈0.25（录屏实测文字 249→138、底色 15→29）
                            alpha = if (isLight) 1f - 0.75f * progress else 1f - 0.55f * progress
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
                        // graphicsLayer 的矩阵会参与坐标换算，点击热区跟着卡片走（同一条行程）
                        .graphicsLayer { translationX = pageTravelPx * progress }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onDrawerOpenChange(false) },
                )
            }
        }
    }
