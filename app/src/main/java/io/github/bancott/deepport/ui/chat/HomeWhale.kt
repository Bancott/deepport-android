package io.github.bancott.deepport.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * 新对话空态的鲸鱼：移植自官方插件 `@deepseek-ai/dsh-client-ui-brand-official`
 * （`src/client/home-whale`，MIT）。
 *
 * 官方那套是一张 137 帧、192×108 的图集，每帧每个像素里：
 * R 通道 = 点的半径系数、G 通道 = 点的不透明度；渲染时逐像素画圆点，
 * 5.7s 播完一条“跃水”动画，作为首页背景水印（官方 CSS 是 18% 透明；这里按实际观感调到 45%，
 * 深色底上才看得清轮廓，又不至于压过问候语）。
 *
 * 这里保留同一套图集和取帧公式，只把“画到哪里”换成 Compose Canvas：
 * 帧在后台线程按 2× 渲染成位图（每帧只有几千个点，几毫秒），再整张贴上去，
 * 比在 Canvas 里直接画两万个点便宜得多，滚动/重组也不会掉帧。
 */
@Composable
fun HomeWhale(
    color: Color,
    playKey: Int,
    modifier: Modifier = Modifier,
    maxAlpha: Float = 0.45f,
) {
    val context = LocalContext.current
    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    var frameAlpha by remember { mutableFloatStateOf(0f) }
    // 图集在空态期间只解码一次：重复点击只是重播，不会反复吃 12MB 和一次 IO
    var frames by remember { mutableStateOf<WhaleFrames?>(null) }
    LaunchedEffect(Unit) {
        frames = withContext(Dispatchers.IO) { WhaleFrames.load(context) }
    }

    LaunchedEffect(playKey, frames) {
        val player = frames ?: return@LaunchedEffect
        if (playKey <= 0) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val progress = (now - start) / WHALE_DURATION_NS
            if (progress >= 1f) break
            val index = (progress * WhaleFrames.FRAME_COUNT).toInt()
                .coerceIn(0, WhaleFrames.FRAME_COUNT - 1)
            frame = withContext(Dispatchers.Default) { player.render(index) }.asImageBitmap()
            // 官方：开头 15 倍、结尾 10 倍速度淡入淡出
            frameAlpha = min(1f, min(progress * 15f, (1f - progress) * 10f)) * maxAlpha
        }
        frame = null
        frameAlpha = 0f
    }

    Canvas(modifier) {
        val image = frame ?: return@Canvas
        if (image.width == 0 || image.height == 0) return@Canvas
        // 官方布局：图案宽度 ≈ 区域宽 × 1.15，垂直中心落在 43% 高度
        val dstWidth = (size.width * 1.15f).toInt()
        val dstHeight = (dstWidth * image.height / image.width.toFloat()).toInt()
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset(
                ((size.width - dstWidth) / 2f).toInt(),
                (size.height * 0.43f - dstHeight / 2f).toInt(),
            ),
            dstSize = IntSize(dstWidth, dstHeight),
            alpha = frameAlpha,
            colorFilter = ColorFilter.tint(color),
            filterQuality = FilterQuality.Low,
        )
    }
}

private const val WHALE_DURATION_NS = 5_700_000_000f

/** 官方图集：12 列 × 192×108 的格子，共 137 帧 */
private class WhaleFrames(private val atlas: Bitmap) {

    private val pixels = IntArray(CELL_W * CELL_H)
    private val paint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
    }

    /** 把某一帧画成 2× 的点阵位图（R=半径系数、G=透明度，均在图集里） */
    fun render(index: Int): Bitmap {
        val out = Bitmap.createBitmap(OUT_W, OUT_H, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(out)
        val originX = (index % COLUMNS) * CELL_W
        val originY = (index / COLUMNS) * CELL_H
        atlas.getPixels(pixels, 0, CELL_W, originX, originY, CELL_W, CELL_H)
        for (y in 0 until CELL_H) {
            val row = y * CELL_W
            for (x in 0 until CELL_W) {
                val color = pixels[row + x]
                val alpha = (color ushr 8) and 0xFF
                if (alpha < 4) continue
                val radius = ((color ushr 16) and 0xFF) / 255f * SCALE
                paint.alpha = alpha
                canvas.drawCircle(
                    (x + 0.5f) * SCALE,
                    (y + 0.5f) * SCALE,
                    radius.coerceAtLeast(0.7f),
                    paint,
                )
            }
        }
        return out
    }

    companion object {
        const val FRAME_COUNT = 137
        private const val CELL_W = 192
        private const val CELL_H = 108
        private const val COLUMNS = 12
        private const val SCALE = 2f
        private const val OUT_W = (CELL_W * SCALE).toInt()
        private const val OUT_H = (CELL_H * SCALE).toInt()

        /** 图集只在播放期间持有，播完即随对象一起回收（12MB，不长期驻留） */
        fun load(context: Context): WhaleFrames? = runCatching {
            context.assets.open("whale/breach_atlas.webp").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()?.let { WhaleFrames(it) }
    }
}
