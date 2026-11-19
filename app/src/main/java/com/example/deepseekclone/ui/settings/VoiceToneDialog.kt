package com.example.deepseekclone.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.SvgIcon
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme
import kotlinx.coroutines.launch

private data class VoiceTone(
    val id: String,
    val name: String,
    val subtitle: String,
)

/**
 * 官方四档朗读音色（与 App 内顺序一致）：
 * 贝壳 / 白浪 / 海星 / 暗潮，接口 id 分别对应 mira / echo / stella / tide。
 */
private val VoiceTones = listOf(
    VoiceTone(id = "mira", name = "贝壳", subtitle = "百变活泼"),
    VoiceTone(id = "echo", name = "白浪", subtitle = "明朗坚定"),
    VoiceTone(id = "stella", name = "海星", subtitle = "俏皮甜美"),
    VoiceTone(id = "tide", name = "暗潮", subtitle = "低沉浑厚"),
)

/** 官方 AppModalBottomSheet：顶部圆角 24dp、内容固定高 586dp、顶栏 56dp */
private val SheetHeight = 586.dp
private val CapsuleWidth = 180.dp
private val CapsuleHeight = 100.dp

/**
 * 朗读音色选择面板，对齐官方 DeepSeek 2.8.0：
 * - ModalBottomSheet（顶部 24dp 圆角、M3 默认 spring 入场）、内容 586dp、顶栏 56dp；
 * - 180×100dp 的全圆角胶囊，内部是缓慢流动的流体（官方是 AGSL/GLSL 着色器，
 *   这里用 Compose Canvas 的三团径向渐变做等价观感，全 API 可用）；
 * - 名称 17sp/400/22、副标题 14sp/400/22、间距 16dp；
 * - 页点 6dp，只做颜色过渡（250ms），尺寸不变。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceToneDialog(
    selected: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    val initial = VoiceTones.indexOfFirst { it.id == selected }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = initial) { VoiceTones.size }
    var draft by remember(selected) { mutableStateOf(VoiceTones.getOrNull(initial)?.id ?: "stella") }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(pager.currentPage) {
        VoiceTones.getOrNull(pager.currentPage)?.let { draft = it.id }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.dialog,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(SheetHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶栏 56dp：标题居中，右端 40dp 关闭
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text = "朗读音色",
                    style = DsText.h2.copy(
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.align(Alignment.Center),
                )
                CircleIconButton(
                    contentDescription = "关闭",
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    containerColor = colors.tile,
                ) {
                    SvgIcon(
                        R.drawable.ic_close_outline_20,
                        tint = colors.textPrimary,
                        size = 20.dp,
                    )
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val side = ((maxWidth - CapsuleWidth) / 2).coerceAtLeast(0.dp)
                HorizontalPager(
                    state = pager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CapsuleHeight),
                    contentPadding = PaddingValues(horizontal = side),
                    pageSpacing = 20.dp,
                ) { page ->
                    FluidCapsule(
                        modifier = Modifier
                            .size(CapsuleWidth, CapsuleHeight)
                            .clip(RoundedCornerShape(CapsuleHeight / 2)),
                        seed = page,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = VoiceTones.firstOrNull { it.id == draft }?.name.orEmpty(),
                style = DsText.body.copy(
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = VoiceTones.firstOrNull { it.id == draft }?.subtitle.orEmpty(),
                style = DsText.caption.copy(
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(40.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VoiceTones.forEachIndexed { index, _ ->
                    val active = index == pager.currentPage
                    val dotColor by animateColorAsState(
                        targetValue = if (active) colors.primary else colors.textTertiary,
                        animationSpec = tween(250),
                        label = "VoicePagerIndicatorColor",
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable(role = Role.Button) {
                                scope.launch { pager.animateScrollToPage(index) }
                            },
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.primary)
                    .clickable(role = Role.Button) {
                        onConfirm(draft)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "确认",
                    style = DsText.button,
                    color = colors.onPrimary,
                )
            }
        }
    }
}

/**
 * 流体胶囊：底色 + 三团缓慢移动的径向渐变，靠三层不同相位的 12s 循环叠加出「云在流动」的感觉。
 * 官方用的是 FluidBlobTextureView（AGSL/GLSL），这里用纯 Compose 等价实现，避免着色器兼容问题。
 */
@Composable
private fun FluidCapsule(modifier: Modifier = Modifier, seed: Int) {
    val transition = rememberInfiniteTransition(label = "fluid$seed")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fluidPhase$seed",
    )
    val base = Color(0xFFE9F1FF)
    val mid = Color(0xFFBFD6FF)
    val deep = Color(0xFF86AEFF)
    Canvas(modifier = modifier) {
        drawRect(base)
        val w = size.width
        val h = size.height
        fun p(t: Float) = (phase + t) % 1f
        // 三团云：各自绕椭圆轨迹漂移
        val blobs = listOf(
            Triple(0.28f, 0.34f, 0.55f),
            Triple(0.72f, 0.62f, 0.42f),
            Triple(0.50f, 0.80f, 0.32f),
        )
        blobs.forEachIndexed { index, (bx, by, alpha) ->
            val t = p(index * 0.33f)
            val angle = t * 2f * Math.PI.toFloat()
            val cx = (bx + 0.10f * kotlin.math.cos(angle)) * w
            val cy = (by + 0.08f * kotlin.math.sin(angle)) * h
            val radius = (0.55f + 0.10f * kotlin.math.sin(angle * 1.7f)) * w
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (index == 0) mid else deep,
                        Color.Transparent,
                    ),
                    center = Offset(cx, cy),
                    radius = radius.coerceAtLeast(1f),
                ),
                radius = radius.coerceAtLeast(1f),
                center = Offset(cx, cy),
                alpha = alpha,
            )
        }
        // 顶部一层白色高光，压出官方的「乳液」质感
        translate(top = -h * 0.18f) {
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.45f, 0f),
                    radius = w * 0.9f,
                ),
                size = Size(w, h * 1.1f),
            )
        }
    }
}
