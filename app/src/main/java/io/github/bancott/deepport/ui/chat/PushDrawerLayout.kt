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
import androidx.compose.runtime.MutableFloatState
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
 * - 会话页右移、缩小到 0.95、内容洗淡到 45%（深）/25%（浅）、30dp 大圆角（有位移即出现）、左缘 1dp 高光；
 * - 整屏压一层 (1−p)×0.2 黑纱（抽屉与卡片缩放露出的底同色，消除方形底），全开时褪尽（官方 ij7 case2 的 ContentScrim）；
 * - 松手吸附：spring(damping 0.9, stiffness 1500)（官方 mj7.a），**速度优先**（快速甩动按方向切换），
 *   否则看进度是否过半。
 */
@Composable
fun PushDrawerLayout(
    drawerOpen: Boolean,
    onDrawerOpenChange: (Boolean) -> Unit,
    /** 外部（消息区手势）驱动的进度；为空时内部自管 */
    externalProgress: MutableFloatState? = null,
    /** 松手但开合状态没变时，外部用这个把进度收到锚点：每次自增触发一次 */
    settleSignal: Int = 0,
    settleTarget: Float = 0f,
    /** 开始一次新手势：自增以取消正在跑的吸附动画，避免动画和手指同时写进度 */
    cancelAnimationSignal: Int = 0,
    modifier: Modifier = Modifier,
    drawerWidthFraction: Float = 0.825f,
    /** 搜索展开层盖在上面时关掉抽屉手势，避免滑动穿透去改 progress */
    gesturesEnabled: Boolean = true,
    drawer: @Composable (Modifier) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = DsTheme.colors
    val isLight = colors.bg.luminance() > 0.5f
    // 直接以目标态起手：从设置页返回时，会话页那层仍记着 drawerOpen=true，
    // 若从 0 起播动画会先闪一帧纯会话界面再"拉回"抽屉，反而不符合预期。
    val progressState = externalProgress
        ?: remember { mutableFloatStateOf(if (drawerOpen) 1f else 0f) }
    var progress by progressState
    val scope = rememberCoroutineScope()
    val velocityTracker = remember { VelocityTracker() }
    var animJob by remember { mutableStateOf<Job?>(null) }
    // 只在跨越阈值时重组一次，避免动画期间每帧重组整个会话页（卡顿根因）。
    // 阈值取 0：手指一动（哪怕 1px）就裁圆角，不会先滑出一段方角再"蹦"出圆角。
    val pageLifted = remember { derivedStateOf { progress > 0f } }

    fun animateProgressTo(target: Float) {
        animJob?.cancel()
        animJob = scope.launch {
            // 官方收尾是弹簧物理（spring damping 0.9 / stiffness 1500，mj7.a），没有固定毫秒数
            animate(
                initialValue = progress,
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.9f, stiffness = 1500f),
            ) { value, _ -> progress = value.coerceIn(0f, 1f) }
        }
    }

    // 外部状态变化（点菜单按钮）时补动画
    LaunchedEffect(drawerOpen) {
        animateProgressTo(if (drawerOpen) 1f else 0f)
    }
    // 外部手势收尾（开合状态没变时）
    LaunchedEffect(settleSignal) {
        if (settleSignal > 0) animateProgressTo(settleTarget)
    }
    // 外部开始拖动：让位给手指
    LaunchedEffect(cancelAnimationSignal) {
        if (cancelAnimationSignal > 0) animJob?.cancel()
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
        // 手机那种大 R 角：官方 20dp，这里放到 30dp；页面拖动时缩到 0.95，视觉半径约 28.5dp
        val liftedShape = remember { RoundedCornerShape(30.dp) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawerOpen, pageTravelPx, gesturesEnabled) {
                    if (!gesturesEnabled) return@pointerInput
                    var dragging = false

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
                                // 1:1 线性跟手：手指位移直接换算成页面行程，无增益、无滞后（官方 AnchoredDraggable 语义）
                                progress = (progress + dragAmount / pageTravelPx).coerceIn(0f, 1f)
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
                            settle(progress > 0.5f)
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

            /* ---------- 1.5 整屏黑纱（抽屉之上、卡片之下） ---------- */
            // 必须盖满整屏，不能只挂在 82.5% 宽的抽屉里：卡片做了 scale 缩小 + 右移，
            // 上/下/右三边会露出最外层那块 drawerSurface 方板；那块板若没被压暗，
            // 就会比左侧抽屉亮一档（浅色实测 248 vs 232），直角边界清晰可见 ——
            // 也就是「圆角卡片底下垫了个矩形」。黑纱挪到全屏后，卡片四周与左侧抽屉
            // 同色、同进度，矩形感消失；卡片在不透明底之上，不受黑纱影响。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 官方 ContentScrim：滑出过程中压 (1−p)×0.2，全开时褪尽。
                    // 颜色固定、alpha 交给 graphicsLayer，避免每帧重组。
                    .graphicsLayer { alpha = (1f - progress) * 0.2f }
                    .background(Color.Black),
            )

            /* ---------- 2. 会话页（上层卡片） ---------- */
            // 不做投影：卡片外侧露出的是被整屏黑纱压暗后的抽屉底色，与左侧抽屉同色，全屏只有两种颜色。
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
}
