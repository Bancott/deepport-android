package io.github.bancott.deepport.ui.chat.markdown

/**
 * 轻量 Markdown 解析：只覆盖官方回答里真实出现的语法
 * —— 标题 / 段落 / 无序列表 / 有序列表 / 引用 / 分割线 / 代码块。
 * 不引第三方 Markdown 库，保证流式追加时成本可控。
 * 渲染由 MessageStream 的原生 TextView（Spannable）承担，这里只负责拆块。
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

