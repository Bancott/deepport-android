package io.github.bancott.deepport.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import io.github.bancott.deepport.R
import java.io.File
import java.io.FileOutputStream

/**
 * 分享图片卡片：把一条回答渲染成 1080 宽的 PNG（深色底 + 鲸鱼标 + 正文 + 免责声明）。
 * 纯 Canvas 绘制，不依赖 WebView；生成到 cache/share 供 FileProvider 分享。
 */
object ShareCard {

    private const val WIDTH = 1080
    private const val PAD = 72f
    private const val MAX_CHARS = 1800
    private const val HEADER_HEIGHT = 120f
    private const val FOOTER_HEIGHT = 120f

    fun render(context: Context, title: String, text: String): File? = runCatching {
        val plain = plainForCard(text)
        val body = plain.trim().let {
            if (it.length > MAX_CHARS) it.take(MAX_CHARS).trimEnd() + "…" else it
        }
        if (body.isBlank()) return null

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9AA0A6")
            textSize = 30f
            typeface = Typeface.DEFAULT
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            typeface = Typeface.DEFAULT
        }
        val brandPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val hintPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8C9094")
            textSize = 26f
        }

        val contentWidth = (WIDTH - PAD * 2).toInt()
        val titleLayout = buildLayout(
            title.ifBlank { "DeepSeek 对话" },
            titlePaint,
            contentWidth,
            maxLines = 2,
            ellipsize = true,
        )
        val bodyLayout = buildLayout(body, bodyPaint, contentWidth, maxLines = Int.MAX_VALUE)
        val footerLayout = buildLayout(
            "来自 DeepSeek 复刻版 · 内容由 AI 生成，仅供参考",
            hintPaint,
            contentWidth,
            maxLines = 2,
            ellipsize = true,
        )

        val height = (
            PAD + HEADER_HEIGHT + titleLayout.height + 28f +
                bodyLayout.height + 56f + 2f + 40f + footerLayout.height + PAD
            ).toInt().coerceAtLeast(640)

        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawPaint(
            Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        Color.parseColor("#1B2440"),
                        Color.parseColor("#0F0F0F"),
                        Color.parseColor("#0F0F0F"),
                    ),
                    floatArrayOf(0f, 0.35f, 1f),
                    Shader.TileMode.CLAMP,
                )
            },
        )

        // 顶部：鲸鱼 + 品牌名 + 右上角提示
        ResourcesCompat.getDrawable(context.resources, R.drawable.ic_deepseek_whale, context.theme)
            ?.mutate()
            ?.apply {
                setBounds(0, 0, 72, 72)
                setTint(Color.WHITE)
            }
            ?.let { logo ->
                canvas.save()
                canvas.translate(PAD, PAD)
                logo.draw(canvas)
                canvas.restore()
            }
        canvas.drawText("DeepSeek", PAD + 96f, PAD + 52f, brandPaint)
        val hint = "AI 生成"
        canvas.drawText(hint, WIDTH - PAD - brandPaint.measureText(hint), PAD + 52f, titlePaint)

        // 标题 + 正文
        var y = PAD + HEADER_HEIGHT
        canvas.save()
        canvas.translate(PAD, y)
        titleLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + 28f

        canvas.save()
        canvas.translate(PAD, y)
        bodyLayout.draw(canvas)
        canvas.restore()
        y += bodyLayout.height + 56f

        // 分隔线 + 页脚
        canvas.drawRect(
            PAD, y, WIDTH - PAD, y + 2f,
            Paint().apply { color = Color.parseColor("#2A2A2A") },
        )
        y += 40f
        canvas.save()
        canvas.translate(PAD, y)
        footerLayout.draw(canvas)
        canvas.restore()

        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        dir.listFiles()?.forEach { if (it.isFile) it.delete() }
        val file = File(dir, "deepseek-share-${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        file
    }.getOrNull()

    /** 分享卡片的正文：保留段落和 `~`（约数），只去掉 Markdown 标记 */
    private fun plainForCard(markdown: String): String = markdown
        .replace(Regex("```[\\s\\S]*?```"), " [代码块] ")
        .replace(Regex("`([^`]*)`"), "$1")
        .replace(Regex("!?\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        .replace(Regex("(?m)^#{1,6}\\s*"), "")
        .replace(Regex("[*_>#|]"), "")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()

    private fun buildLayout(        text: String,
        paint: TextPaint,
        width: Int,
        maxLines: Int,
        ellipsize: Boolean = false,
    ): StaticLayout = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.35f)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .setEllipsize(if (ellipsize) TextUtils.TruncateAt.END else null)
        .build()
}
