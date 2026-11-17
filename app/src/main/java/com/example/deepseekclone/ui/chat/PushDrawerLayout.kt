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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * 官方抽屉动画："推页式"。
 * 打开方式有两种：点左上角菜单、或从左侧边缘右滑；打开过程中页面整体右移 82%，
 * 底色换成"被抬起"的表面色，内容透明度随进度下降到 38%，四角切 16dp 圆角。
 * 打开状态下向左滑动或点击右侧露出的页面区域即可关闭。
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
        fun settle(open: Boolean) {
            onDrawerOpenChange(open)
            scope.launch {
                progress.animateTo(
                    targetValue = if (open) 1f else 0f,
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                )
            }
        }

        val density = LocalDensity.current
        val drawerWidth = maxWidth * drawerWidthFraction
        val drawerWidthPx = with(density) { drawerWidth.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(drawerOpen, drawerWidthPx) {
                    var dragging = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // 关闭态：页面任意位置右滑都可打开（垂直滚动不受影响）；
                            // 打开态：任意位置左滑关闭。避开系统返回手势的极左边缘。
                            dragging = true
                        },
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
            // 被推开的"卡片"：底色是提亮后的表面色，内容整体淡出
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.pushedPage)
                    .graphicsLayer {
                        // 页面比抽屉"慢半拍"（p^1.7），同时轻微缩小、变淡 —— 形成前后的纵深感，
                        // 不是单纯的水平平移。
                        val pageP = progress.value.pow(1.7f)
                        translationX = drawerWidthPx * pageP
                        val s = 1f - 0.035f * pageP
                        scaleX = s
                        scaleY = s
                        if (progress.value > 0.01f) {
                            clip = true
                            shape = RoundedCornerShape(16.dp)
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                        val pageP = progress.value.pow(1.7f)
                        alpha = 1f - 0.62f * pageP * pageP
                    },
                ) {
                    content()
                }
            }

            // 点击右侧露出的页面区域关闭
            if (progress.value > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset { IntOffset((drawerWidthPx * progress.value).roundToInt(), 0) }
                        .clickable { onDrawerOpenChange(false) },
                )
            }

            Box(
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .offset {
                        IntOffset((-drawerWidthPx + drawerWidthPx * progress.value).roundToInt(), 0)
                    }
                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)),
            ) {
                drawer(Modifier.fillMaxSize())
            }
        }
    }
}



