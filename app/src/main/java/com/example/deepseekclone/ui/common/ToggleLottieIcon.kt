package com.example.deepseekclone.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieAnimatable
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty

/** 与 APK 一致的 chip 图标尺寸（Lottie 原始画布就是 16×16） */
private val IconSize = 16.dp

/**
 * 可正放 / 倒放的 Lottie 状态图标，对应官方 APK 的 chip 动效：
 *
 * - 选中：`speed = 1` 正向播放；取消：`speed = -1` 倒放；
 * - 倒放从**当前进度**继续（`initialProgress = animatable.progress`），不会跳回 1；
 * - composition 首次加载完成时用 `snapTo` 直接对齐目标态，不播动画（避免冷启动闪动）；
 * - Lottie 内路径是硬编码深色，这里用 `LottieProperty.COLOR_FILTER` +
 *   `SimpleColorFilter` 按 `"**"` 全路径染色，tint 取调用方的 contentColor。
 */
@Composable
fun ToggleLottieIcon(
    iconRes: Int,
    selected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(iconRes))
    val animatable = rememberLottieAnimatable()
    var initialized by remember(iconRes) { mutableStateOf(false) }

    // composition 从 null → 加载完成时也要跑一次，所以把它放进 key；
    // selected 在加载期间变化时，首次 snapTo 会直接落到最新的目标态。
    LaunchedEffect(iconRes, composition, selected) {
        val loaded = composition ?: return@LaunchedEffect
        if (!initialized) {
            animatable.snapTo(
                composition = loaded,
                progress = if (selected) 1f else 0f,
            )
            initialized = true
        } else {
            animatable.animate(
                composition = loaded,
                speed = if (selected) 1f else -1f,
                initialProgress = animatable.progress,
            )
        }
    }

    val colorFilter = remember(tint) { SimpleColorFilter(tint.toArgb()) }
    LottieAnimation(
        composition = composition,
        progress = { animatable.progress },
        modifier = modifier.size(IconSize),
        dynamicProperties = rememberLottieDynamicProperties(
            // keyPath 是 vararg：Kotlin 不允许以命名形式传单个元素，只能按位置传。
            rememberLottieDynamicProperty(
                LottieProperty.COLOR_FILTER,
                colorFilter,
                "**",
            ),
        ),
    )
}
