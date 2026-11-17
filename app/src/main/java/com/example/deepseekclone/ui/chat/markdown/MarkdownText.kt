package com.example.deepseekclone.ui.chat.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 轻量 Markdown 渲染：只覆盖官方回答里真实出现的语法
 * —— 标题 / 段落 / 无序列表 / 有序列表 / 引用 / 分割线 / 代码块 / 行内 code / 粗体。
 * 不引第三方 Markdown 库，保证排版与截图一致、且流式追加时成本可控。
 */
internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Para(val text: String) : MdBlock
    data class Bullets(val items: List<String>) : MdBlock
    data class Numbers(val items: List<Pair<String, String>>) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val code: String) : MdBlock
    data object Divider : MdBlock
}

private val BulletRegex = Regex("^[-*•]\\s+(.*)$")
private val NumberedRegex = Regex("^(\\d+)[.、)]\\s+(.*)$")
private val DividerRegex = Regex("^\\s*(-{3,}|\\*{3,})\\s*$")

private fun needsSpace(prev: Char, next: Char): Boolean =
    prev.isAsciiWord() && next.isAsciiWord()

private fun Char.isAsciiWord(): Boolean =
    this in 'a'..'z' || this in 'A'..'Z' || this in '0'..'9'

internal fun parseMarkdown(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = markdown.split('\n')
    val para = StringBuilder()
    var i = 0

    fun flush() {
        if (para.isNotBlank()) {
            blocks.add(MdBlock.Para(para.toString().trim()))
            para.clear()
        }
    }

    while (i < lines.size) {
        val line = lines[i].trimEnd()

        when {
            line.startsWith("```") -> {
                flush()
                i++
                val code = StringBuilder()
                while (i < lines.size && !lines[i].startsWith("```")) {
                    code.appendLine(lines[i])
                    i++
                }
                blocks.add(MdBlock.Code(code.toString().trimEnd()))
            }

            line.isBlank() -> flush()

            DividerRegex.matches(line) -> {
                flush()
                blocks.add(MdBlock.Divider)
            }

            line.startsWith("### ") -> {
                flush()
                blocks.add(MdBlock.Heading(3, line.removePrefix("### ").trim()))
            }

            line.startsWith("## ") -> {
                flush()
                blocks.add(MdBlock.Heading(2, line.removePrefix("## ").trim()))
            }

            line.startsWith("# ") -> {
                flush()
                blocks.add(MdBlock.Heading(1, line.removePrefix("# ").trim()))
            }

            line.startsWith("> ") -> {
                flush()
                blocks.add(MdBlock.Quote(line.removePrefix("> ").trim()))
            }

            BulletRegex.matches(line) -> {
                flush()
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val m = BulletRegex.find(lines[i].trimEnd()) ?: break
                    items.add(m.groupValues[1].trim())
                    i++
                }
                blocks.add(MdBlock.Bullets(items))
                continue
            }

            NumberedRegex.matches(line) -> {
                flush()
                val items = mutableListOf<Pair<String, String>>()
                while (i < lines.size) {
                    val m = NumberedRegex.find(lines[i].trimEnd()) ?: break
                    items.add(m.groupValues[1] to m.groupValues[2].trim())
                    i++
                }
                blocks.add(MdBlock.Numbers(items))
                continue
            }

            else -> {
                val trimmed = line.trim()
                if (para.isNotEmpty() && trimmed.isNotEmpty()) {
                    if (needsSpace(para.last(), trimmed.first())) para.append(' ')
                }
                para.append(trimmed)
            }
        }
        i++
    }
    flush()
    return blocks
}

/** 行内样式：`code` 与 **粗体** */
private fun inline(text: String, codeBg: Color, color: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val end = text.indexOf("**", i + 2)
            if (end > i) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i) {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        background = codeBg,
                        color = color,
                    ),
                ) {
                    append(" ")
                    append(text.substring(i + 1, end))
                    append(" ")
                }
                i = end + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    val blocks = parseMarkdown(markdown)
    Column(modifier = modifier.fillMaxWidth()) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is MdBlock.Heading -> {
                    Text(
                        text = inline(block.text, colors.surface2, color),
                        style = if (block.level <= 2) DsText.h1 else DsText.h2,
                        color = color,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 22.dp, bottom = 6.dp),
                    )
                }

                is MdBlock.Para -> {
                    Text(
                        text = inline(block.text, colors.surface2, color),
                        style = DsText.body,
                        color = color,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 16.dp),
                    )
                }

                is MdBlock.Bullets -> {
                    Column(Modifier.padding(top = if (index == 0) 0.dp else 16.dp)) {
                        block.items.forEachIndexed { itemIndex, item ->
                            Row(Modifier.padding(top = if (itemIndex == 0) 0.dp else 12.dp)) {
                                Text(
                                    text = "•",
                                    style = DsText.body.copy(fontSize = 13.sp),
                                    color = color,
                                    modifier = Modifier.width(12.dp),
                                )
                                Text(
                                    text = inline(item, colors.surface2, color),
                                    style = DsText.body,
                                    color = color,
                                )
                            }
                        }
                    }
                }

                is MdBlock.Numbers -> {
                    Column(Modifier.padding(top = if (index == 0) 0.dp else 16.dp)) {
                        block.items.forEachIndexed { itemIndex, part ->
                            Row(Modifier.padding(top = if (itemIndex == 0) 0.dp else 12.dp)) {
                                Text(
                                    text = part.first + ".",
                                    style = DsText.body,
                                    color = color,
                                    modifier = Modifier.width(24.dp),
                                )
                                Text(
                                    text = inline(part.second, colors.surface2, color),
                                    style = DsText.body,
                                    color = color,
                                )
                            }
                        }
                    }
                }

                is MdBlock.Quote -> {
                    Row(
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .background(colors.outline),
                        )
                        Text(
                            text = inline(block.text, colors.surface2, color),
                            style = DsText.bodyTight,
                            color = colors.textSecondary,
                        )
                    }
                }

                is MdBlock.Code -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = if (index == 0) 0.dp else 16.dp)
                            .background(colors.surface2, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        Text(
                            text = block.code,
                            style = DsText.bodyTight.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                            ),
                            color = color,
                        )
                    }
                }

                MdBlock.Divider -> {
                    HorizontalDivider(
                        color = colors.outline,
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            }
        }    }
}



