package io.github.bancott.deepport.ui.settings

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
import androidx.annotation.RequiresApi
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.CircleIconButton
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme
import kotlinx.coroutines.launch

private data class VoiceTone(
    val id: String,
    val name: String,
    val subtitle: String,
    /** 官方 fluid_blob 的 color1/color2/color3 三色（APK 内置三套 + 截图里「贝壳」的浅蓝白） */
    val palette: List<Color>,
)

/**
 * 官方四档朗读音色（与 App 内顺序一致）：
 * 贝壳 / 白浪 / 海星 / 暗潮，接口 id 分别对应 mira / echo / stella / tide。
 * 调色板取自 APK 的 FluidBlobDefaults（蓝/珊瑚/绿三套）与官方截图的贝壳浅蓝白。
 */
private val VoiceTones = listOf(
    VoiceTone(
        id = "mira", name = "贝壳", subtitle = "百变活泼",
        palette = listOf(Color(0xFFF4F8FF), Color(0xFFCFE3FF), Color(0xFF9CC0FF)),
    ),
    VoiceTone(
        id = "echo", name = "白浪", subtitle = "明朗坚定",
        palette = listOf(Color(0xFFD9EAF5), Color(0xFF3899B8), Color(0xFF1A6B94)),
    ),
    VoiceTone(
        id = "stella", name = "海星", subtitle = "俏皮甜美",
        palette = listOf(Color(0xFFF2D8A7), Color(0xFFD4764E), Color(0xFF8B3A3A)),
    ),
    VoiceTone(
        id = "tide", name = "暗潮", subtitle = "低沉浑厚",
        palette = listOf(Color(0xFFA8C9A5), Color(0xFF5B8C5A), Color(0xFF2D5A3D)),
    ),
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

            // 官方：胶囊顶边在内容区 145dp（整面板 201dp = 56dp 顶栏 + 145dp）
            Spacer(Modifier.height(145.dp))

            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val side = ((maxWidth - CapsuleWidth) / 2).coerceAtLeast(0.dp)
                // 只用一个固定胶囊：翻页手势交给透明的 Pager，
                // 胶囊内的流体颜色按翻页进度在相邻两档之间插值，画面平滑过渡。
                val position = pager.currentPage + pager.currentPageOffsetFraction
                val palette = interpolatePalette(
                    VoiceTones[position.toInt().coerceIn(0, VoiceTones.lastIndex)].palette,
                    VoiceTones[(position.toInt() + 1).coerceIn(0, VoiceTones.lastIndex)].palette,
                    position - position.toInt(),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(CapsuleHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    FluidCapsule(
                        modifier = Modifier
                            .size(CapsuleWidth, CapsuleHeight)
                            .clip(RoundedCornerShape(CapsuleHeight / 2)),
                        palette = palette,
                    )
                }
                HorizontalPager(
                    state = pager,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CapsuleHeight),
                    contentPadding = PaddingValues(horizontal = side),
                    pageSpacing = 20.dp,
                ) { /* 透明页：只负责手势 */ }
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
 * 流体胶囊：官方 DeepSeek 2.8.0 的 `FluidBlobTextureView` 同款。
 *
 * 着色器直接使用 APK 里的 `assets/shaders/fluid_blob.agsl`（已原样放进 res/raw），
 * uniform 默认值与官方一致：scale 1.74、rotation 0.2333、offset (0,0.8)、colorCount 3、
 * proportion 0.45、softness 1.0、shape 1（STRIPES）、shapeScale 0.18、distortion 0.55、
 * swirl 0.08、swirlIterations 10、grain 0；time = patternSpeed(0.8)/100 × 秒。
 *
 * API 33+ 走 `RuntimeShader`（AGSL）；26–32 没有 RuntimeShader，退化为同色系的渐变云，
 * 保证不崩溃且观感一致。
 */
@Composable
private fun FluidCapsule(modifier: Modifier = Modifier, palette: List<Color>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        AgslFluidCapsule(modifier = modifier, palette = palette)
    } else {
        GradientFluidCapsule(modifier = modifier, palette = palette)
    }
}

/** 相邻音色的三色 palette 线性插值：翻页到一半时颜色正好是两者的混合 */
private fun interpolatePalette(a: List<Color>, b: List<Color>, t: Float): List<Color> {
    val f = t.coerceIn(0f, 1f)
    return List(3) { i ->
        androidx.compose.ui.graphics.lerp(a[i], b[i], f)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun AgslFluidCapsule(modifier: Modifier = Modifier, palette: List<Color>) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val source = remember {
        context.resources.openRawResource(R.raw.fluid_blob).bufferedReader().use { it.readText() }
    }
    // 着色器编译失败时回退到渐变实现，避免个别 ROM 上直接崩
    val shader = remember(source) { runCatching { RuntimeShader(source) }.getOrNull() }
    if (shader == null) {
        GradientFluidCapsule(modifier = modifier, palette = palette)
        return
    }

    val transition = rememberInfiniteTransition(label = "fluid")
    // 官方 time = 0.8/100 × 秒；用 125s 走满 1.0 的循环来供给，避免每帧读系统时钟
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 125_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fluidClock",
    )
    Canvas(modifier = modifier) {
        // AGSL 里是 `uniform float4 colorN;`（不是 layout(color)），只能用 float 通道设置；
        // 用 setColorUniform 会抛 "attempting to set a color uniform using the non-color specific APIs"
        shader.setFloatUniform("color1", palette[0].red, palette[0].green, palette[0].blue, 1f)
        shader.setFloatUniform("color2", palette[1].red, palette[1].green, palette[1].blue, 1f)
        shader.setFloatUniform("color3", palette[2].red, palette[2].green, palette[2].blue, 1f)
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("offset", 0f, 0.8f)
        shader.setFloatUniform("time", clock * 0.008f)
        shader.setFloatUniform("pixelRatio", density)
        shader.setFloatUniform("scale", 1.74f)
        shader.setFloatUniform("rotation", 0.23333333f)
        shader.setFloatUniform("colorCount", 3f)
        shader.setFloatUniform("grain", 0f)
        shader.setFloatUniform("proportion", 0.45f)
        shader.setFloatUniform("softness", 1f)
        shader.setFloatUniform("shape", 1f)
        shader.setFloatUniform("shapeScale", 0.18f)
        shader.setFloatUniform("distortion", 0.55f)
        shader.setFloatUniform("swirl", 0.08f)
        shader.setFloatUniform("swirlIterations", 10f)
        drawRect(brush = ShaderBrush(shader))
    }
}

/** API < 33 的退化实现：同色系的三团渐变云，12s 循环。 */
@Composable
private fun GradientFluidCapsule(modifier: Modifier = Modifier, palette: List<Color>) {
    val transition = rememberInfiniteTransition(label = "fluidFallback")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fluidFallbackPhase",
    )
    val base = palette[0]
    val mid = palette[1]
    val deep = palette[2]
    Canvas(modifier = modifier) {
        drawRect(base)
        val w = size.width
        val h = size.height
        fun p(t: Float) = (phase + t) % 1f
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
                    colors = listOf(if (index == 0) mid else deep, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius.coerceAtLeast(1f),
                ),
                radius = radius.coerceAtLeast(1f),
                center = Offset(cx, cy),
                alpha = alpha,
            )
        }
    }
}