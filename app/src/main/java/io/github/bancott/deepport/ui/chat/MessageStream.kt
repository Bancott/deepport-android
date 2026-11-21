package io.github.bancott.deepport.ui.chat

import io.github.bancott.deepport.R

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import android.text.style.LineHeightSpan
import android.text.style.StyleSpan
import android.text.style.TabStopSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.widget.TextView
import java.text.BreakIterator
import kotlin.math.roundToInt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.github.bancott.deepport.ui.chat.markdown.MdBlock
import io.github.bancott.deepport.ui.chat.web.WebMessageView
import io.github.bancott.deepport.ui.chat.markdown.parseMarkdown
import io.github.bancott.deepport.ui.common.GlobeGlyph
import io.github.bancott.deepport.ui.common.ReadingBars
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsColors
import io.github.bancott.deepport.ui.theme.DsDarkColors
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

/** 正在（系统选择模式）选中的那条消息：记住长按点，用它定位系统选择从哪开始 */
data class MessageSelection(
    val messageId: String,
    val text: String,
    /** 长按点的窗口坐标；进入选择模式时换算成 TextView 内部坐标去合成长按 */
    val pressWindow: Offset,
)

/** 是否含需要 WebView 才能渲染的富内容：公式、GFM 表格、代码块、SVG */
internal fun isRichMarkdown(text: String): Boolean {
    if (text.contains("$$")) return true
    // 官方渲染用 llm-math，LLM 常写 \(...\)、\[...\]、\begin{equation}
    if (text.contains("\\(") || text.contains("\\[") || text.contains("\\begin{")) return true
    if (text.contains("```")) return true
    if (Regex("\\$[^$\\n]{1,200}\\$").containsMatchIn(text)) return true
    val tableLines = text.lineSequence().count { it.trimStart().startsWith("|") }
    return tableLines >= 2
}

/** 会话消息流：LazyColumn + 稳定 key，用户气泡与 AI 通栏回答两种形态。 */
@Composable
fun MessageStream(
    messages: List<MessageUi>,
    sessionTokens: Int,
    modifier: Modifier = Modifier,
    readingMessageId: String? = null,
    readingState: ReadAloudState = ReadAloudState.Idle,
    selection: MessageSelection? = null,
    onSelectionChange: (MessageSelection?) -> Unit = {},
    /** 选择模式下拉起时回报正文在窗口里的矩形，供「点选区外退出」判断 */
    onSelectionBounds: (Rect) -> Unit = {},
    /** 系统选择工具栏的「朗读」：读选区，走 App 内置 TTS */
    onSpeakSelection: (String) -> Unit = {},
    onToggleReasoning: (MessageUi) -> Unit = {},
    onCopy: (MessageUi) -> Unit = {},
    onSpeak: (MessageUi) -> Unit = {},
    onShare: (MessageUi) -> Unit = {},
    onRegenerate: (MessageUi) -> Unit = {},
    onRetry: (MessageUi) -> Unit = {},
    onEdit: (MessageUi) -> Unit = {},
    onSourcesClick: (MessageUi) -> Unit = {},
    onBranchPrev: (MessageUi) -> Unit = {},
    onBranchNext: (MessageUi) -> Unit = {},
    /** 正在编辑的消息 id：其它消息淡化到 0.9、点一下取消编辑 */
    editingId: String? = null,
    onCancelEdit: () -> Unit = {},
) {
    // 自动跟随：用户停在底部时，新内容到达自动滚到底；一旦手动上滑即暂停跟随
    val listState = rememberLazyListState()
    var follow by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()
            // 必须同时满足「最后一条在视口里」和「它的底边已经贴近视口底」才算跟到底部。
            // 只看 index 的话，回答变高（WebView）后最后一条永远可见，用户根本滑不上去，
            // 一有新 token 就被拽回底部。
            last != null &&
                last.index >= info.totalItemsCount - 1 &&
                (last.offset + last.size - info.viewportEndOffset) <= 160
        }.collect { follow = it }
    }
    LaunchedEffect(messages.size, messages.lastOrNull()?.text?.length, messages.lastOrNull()?.reasoning?.length) {
        if (follow && messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex, scrollOffset = 10_000)
        }
    }
    // 编辑态：把被编辑的那条"吸"到输入框正上方（官方 SnapToInputFieldEffect，-8dp 补偿）——
    // 先滚到目标项，再补滚把该项底部钉到视口底 8dp 之上，而不是甩到列表顶
    val density = LocalDensity.current
    LaunchedEffect(editingId) {
        val id = editingId ?: return@LaunchedEffect
        val index = messages.indexOfFirst { it.id == id }
        if (index >= 0) {
            follow = false
            listState.animateScrollToItem(index)
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
            if (item != null) {
                val gapPx = with(density) { 8.dp.toPx() }
                // 目标：该项底边落在视口底上方 8dp（输入框正上方）
                val desiredTop = info.viewportEndOffset - gapPx - item.size
                val delta = item.offset - desiredTop
                if (delta != 0f) {
                    listState.animateScrollBy(delta)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = 10.dp),
    ) {
        items(messages, key = { it.id }) { message ->
            // 非编辑目标：alpha 0.9，点一下取消编辑（官方 CancelEditEffect）
            val dimmed = editingId != null && editingId != message.id
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (dimmed) 0.9f else 1f)
                    .then(
                        if (dimmed) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCancelEdit,
                            )
                        } else {
                            Modifier
                        },
                    ),
            ) {
                when (message.role) {
                    ChatRole.User -> UserMessage(
                        message = message,
                        selection = selection?.takeIf { it.messageId == message.id },
                        onSelectionChange = onSelectionChange,
                        onSelectionBounds = onSelectionBounds,
                        onSpeakSelection = onSpeakSelection,
                        onEdit = { onEdit(message) },
                    )
                    ChatRole.Assistant -> AssistantMessage(
                        message = message,
                        sessionTokens = sessionTokens,
                        readingState = if (message.id == readingMessageId) {
                            readingState
                        } else {
                            ReadAloudState.Idle
                        },
                        onToggleReasoning = { onToggleReasoning(message) },
                        onCopy = { onCopy(message) },
                        onSpeak = { onSpeak(message) },
                        onShare = { onShare(message) },
                        onRegenerate = { onRegenerate(message) },
                        onRetry = { onRetry(message) },
                        onSourcesClick = { onSourcesClick(message) },
                        onBranchPrev = { onBranchPrev(message) },
                        onBranchNext = { onBranchNext(message) },
                    )
                }
            }
        }
    }
}

/* ----------------------------- 用户气泡 ----------------------------- */

@Composable
private fun UserMessage(
    message: MessageUi,
    selection: MessageSelection?,
    onSelectionChange: (MessageSelection?) -> Unit,
    onSelectionBounds: (Rect) -> Unit,
    onSpeakSelection: (String) -> Unit,
    onEdit: () -> Unit,
) {
    val colors = DsTheme.colors
    var textView by remember { mutableStateOf<TextView?>(null) }
    var viewOrigin by remember { mutableStateOf(Offset.Zero) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 16.dp, top = 14.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        val menuExpanded = remember { mutableStateOf(false) }
        MessageLongPressBox(
            message = message,
            expanded = menuExpanded.value,
            onExpandedChange = { menuExpanded.value = it },
            // 选择模式下手势交给文本自身（拖手柄 / 长按换词），不能再弹消息菜单
            enabled = selection == null,
            onEdit = onEdit,
            onSelectText = { pressWindow ->
                // 只记消息和长按点：真正的选区由系统在选择模式里自己决定
                onSelectionChange(
                    MessageSelection(
                        messageId = message.id,
                        text = message.text,
                        pressWindow = pressWindow,
                    ),
                )
            },
        ) {
            Column(horizontalAlignment = Alignment.End) {
                if (message.attachments.isNotEmpty()) {
                    MessageAttachments(message.attachments)
                }
                if (message.text.isNotBlank()) {
                    Surface(
                        color = colors.bubble,
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        MessageBodyText(
                            text = message.text,
                            markdown = false,
                            selecting = selection != null,
                            pressWindow = selection?.pressWindow,
                            color = colors.textPrimary,
                            onViewReady = { textView = it },
                            onViewOrigin = { viewOrigin = it },
                            onSelectionFinished = { onSelectionChange(null) },
                            onSelectionBounds = onSelectionBounds,
                            onSpeakSelection = onSpeakSelection,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 消息正文渲染 + App 内自绘选择。
 *
 * 普通态就是一个 TextView（Spannable markdown）；长按菜单点「选择文本」后进入选择态：
 * 预选长按处所在段落，画选区高亮、两个可拖手柄和「复制 / 全选 / 朗读」浮层。
 * 选择完全在 App 内实现（NativeSelectionLayer），不依赖 textIsSelectable / 系统 ActionMode。
 */
@Composable
private fun MessageBodyText(
    text: String,
    markdown: Boolean,
    selecting: Boolean,
    pressWindow: Offset?,
    color: Color,
    onViewReady: (TextView) -> Unit = {},
    onViewOrigin: (Offset) -> Unit = {},
    onSelectionFinished: () -> Unit = {},
    onSelectionBounds: (Rect) -> Unit = {},
    onSpeakSelection: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = DsTheme.colors
    val density = LocalDensity.current
    val context = LocalContext.current
    val isDark = colors === DsDarkColors
    val textColor = color.toArgb()
    val highlightArgb = (if (isDark) Color(0xFF33426B) else Color(0x664C6EF6)).toArgb()
    val handleColor = if (isDark) Color(0xFF6599FE) else Color(0xFF4C6EF6)
    val metrics = remember(colors, density, compact) { SpanMetrics(density, colors, compact) }
    val content = remember(text, markdown, metrics) {
        buildBodySpannable(text, markdown, metrics)
    }
    var tv by remember { mutableStateOf<TextView?>(null) }
    var armed by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf<TextRange?>(null) }
    var boxOrigin by remember { mutableStateOf(Offset.Zero) }
    val finished by rememberUpdatedState(onSelectionFinished)
    val boundsReport by rememberUpdatedState(onSelectionBounds)
    val speak by rememberUpdatedState(onSpeakSelection)

    fun paintHighlight(r: TextRange?) {
        val sp = tv?.text as? Spannable ?: return
        sp.getSpans(0, sp.length, BackgroundColorSpan::class.java).forEach(sp::removeSpan)
        if (r != null && r.length > 0) {
            val s = r.start.coerceIn(0, sp.length)
            val e = r.end.coerceIn(s, sp.length)
            if (e > s) {
                sp.setSpan(
                    BackgroundColorSpan(highlightArgb),
                    s,
                    e,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }

    // 选区变化后重挂高亮（内容重建后也要补一次）
    LaunchedEffect(range, content, tv) { paintHighlight(range) }

    Box(
        modifier = modifier.onGloballyPositioned { boxOrigin = it.positionInWindow() },
    ) {
        AndroidView<TextView>(
            factory = { ctx ->
                TextView(ctx).apply {
                    includeFontPadding = false
                    setPadding(0, 0, 0, 0)
                    setTextColor(textColor)
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, metrics.textPx)
                    showSoftInputOnFocus = false
                    setText(content, TextView.BufferType.SPANNABLE)
                }
            },
            update = { view ->
                onViewReady(view)
                if (tv !== view) tv = view
                if (view.text !== content) {
                    view.setText(content, TextView.BufferType.SPANNABLE)
                }
                if (selecting && !armed) {
                    armed = true
                    view.postDelayed({
                        val loc = IntArray(2)
                        view.getLocationInWindow(loc)
                        boundsReport(
                            Rect(
                                loc[0].toFloat(),
                                loc[1].toFloat(),
                                (loc[0] + view.width).toFloat(),
                                (loc[1] + view.height).toFloat(),
                            ),
                        )
                        val local = textPointFor(view, pressWindow)
                        val offset = view.getOffsetForPosition(local.x, local.y)
                        range = paragraphRange(view.text.toString(), offset)
                    }, 60)
                } else if (!selecting && armed) {
                    armed = false
                    range = null
                    paintHighlight(null)
                }
            },
            modifier = (if (markdown) Modifier.fillMaxWidth() else Modifier)
                .onGloballyPositioned { onViewOrigin(it.positionInWindow()) },
        )

        val view = tv
        val sel = range
        if (view != null && sel != null && sel.length > 0) {
            val loc = IntArray(2)
            view.getLocationInWindow(loc)
            val origin = Offset(loc[0] - boxOrigin.x, loc[1] - boxOrigin.y)
            NativeSelectionLayer(
                view = view,
                range = sel,
                origin = origin,
                handleColor = handleColor,
                onRangeChange = { range = it },
                onCopy = {
                    val s = sel.start.coerceIn(0, view.text.length)
                    val e = sel.end.coerceIn(s, view.text.length)
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("selection", view.text.subSequence(s, e)))
                    finished()
                },
                onSelectAll = {
                    range = TextRange(0, view.text.length)
                },
                onSpeak = {
                    val s = sel.start.coerceIn(0, view.text.length)
                    val e = sel.end.coerceIn(s, view.text.length)
                    speak(view.text.subSequence(s, e).toString())
                    finished()
                },
                onDismiss = { finished() },
            )
        }
    }
}

/** 由字符位置扩到所在段落（换行边界） */
private fun paragraphRange(text: String, anchor: Int): TextRange {
    if (text.isEmpty()) return TextRange(0)
    val a = anchor.coerceIn(0, text.length)
    var start = a
    while (start > 0 && text[start - 1] != '\n') start--
    var end = a
    while (end < text.length && text[end] != '\n') end++
    while (start < end && text[start].isWhitespace()) start++
    while (end > start && text[end - 1].isWhitespace()) end--
    return if (start < end) TextRange(start, end) else TextRange(0, text.length)
}

/**
 * 把长按点换算成 TextView 内部坐标，并夹到「真正有文字」的那一行里：
 * 长按可能落在段间空白、代码块内边距这类非文字区域。
 */
private fun textPointFor(view: TextView, pressWindow: Offset?): Offset {
    val loc = IntArray(2)
    view.getLocationInWindow(loc)
    val raw = pressWindow?.let { Offset(it.x - loc[0], it.y - loc[1]) }
        ?: Offset(view.width / 2f, view.height / 2f)
    val layout = view.layout ?: return raw
    if (layout.lineCount == 0) return raw
    val x = raw.x.coerceIn(0f, (view.width - 1).coerceAtLeast(1).toFloat())
    val y = raw.y.coerceIn(0f, (view.height - 1).coerceAtLeast(1).toFloat())
    val line = layout.getLineForVertical(y.toInt()).coerceIn(0, layout.lineCount - 1)
    val left = layout.getLineLeft(line)
    val right = layout.getLineRight(line)
    val top = layout.getLineTop(line).toFloat()
    val bottom = layout.getLineBottom(line).toFloat()
    val cx = if (right > left) x.coerceIn(left, right - 1f) else left
    val cy = y.coerceIn(top, (bottom - 1f).coerceAtLeast(top))
    return Offset(cx, cy)
}


/**
 * Markdown → Spannable 用的全部排版常量，数值与原 Compose 版 MarkdownText 同源：
 * 正文 17sp/28sp，气泡 17sp/26sp，标题 22/30、20/28，代码 14sp/22sp（框内边距 14dp）。
 *
 * [compact] = 思考区排版：官方思考区是独立的一套 markdown typography，
 * 10 个槽位（h1/h2/h3/正文/列表/引用）全部 15sp / 行高 24sp，块间距 6dp。
 */
private class SpanMetrics(
    private val density: Density,
    colors: DsColors,
    private val compact: Boolean = false,
) {
    private fun sp(v: Float) = with(density) { v.sp.toPx() }
    private fun dp(v: Float) = with(density) { v.dp.toPx() }

    /** 基础字号（未被块级样式覆盖的文字都是它） */
    val textPx = sp(if (compact) 15f else 17f)
    /** 正文行高 / 气泡行高 */
    val bodyLinePx = sp(if (compact) 24f else 28f)
    val tightLinePx = sp(if (compact) 24f else 26f)
    /** 一二级标题 / 三级标题（思考区不放大标题） */
    val h1SizePx = sp(if (compact) 15f else 22f)
    val h1LinePx = sp(if (compact) 24f else 30f)
    val h2SizePx = sp(if (compact) 15f else 20f)
    val h2LinePx = sp(if (compact) 24f else 28f)
    /** 代码块字号 / 行高 / 行内 code 字号 */
    val codeSizePx = sp(if (compact) 13f else 14f)
    val codeLinePx = sp(if (compact) 20f else 22f)
    val inlineCodePx = sp(if (compact) 14f else 15f)

    /** 块间距（段落/列表/代码块之间） */
    val gapPx = dp(if (compact) 6f else 16f)
    /** 标题上方 / 标题下方附加间距 */
    val headingTopPx = dp(if (compact) 6f else 22f)
    val headingBottomPx = dp(if (compact) 6f else 6f)
    /** 列表项间距 */
    val itemGapPx = dp(if (compact) 6f else 12f)
    /** 代码块内边距 / 圆角 */
    val codePadPx = dp(14f)
    val codeRadiusPx = dp(12f)
    /** 分割线上下留白 / 线高 */
    val dividerPadPx = dp(20f)
    val dividerLinePx = dp(1f)
    /** 引用竖线宽 / 线到文字的间隙 */
    val quoteBarPx = dp(1f)
    val quoteGapPx = dp(14f)
    /** 列表缩进：圆点列 12dp、序号列 24dp，圆点直径 5dp */
    val bulletIndentPx = dp(12f)
    val bulletDotPx = dp(5f)
    val numberIndentPx = dp(24f)

    val codeBg = colors.surface2.toArgb()
    val outline = colors.outline.toArgb()
    val quoteColor = colors.textSecondary.toArgb()
    val bulletColor = colors.textPrimary.toArgb()
}

/** 固定行高：把整行的 ascent/descent 等比缩放到目标行高（与 Compose 的 Proportional 行为一致） */
private class FixedLineSpan(private val heightPx: Int) : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        v: Int,
        fm: Paint.FontMetricsInt,
    ) {
        val current = fm.descent - fm.ascent
        if (current <= 0 || current == heightPx) return
        val ratio = heightPx.toFloat() / current
        val ascent = Math.round(fm.ascent * ratio)
        fm.ascent = ascent
        fm.descent = ascent + heightPx
        fm.top = minOf(fm.top, fm.ascent)
        fm.bottom = maxOf(fm.bottom, fm.descent)
    }
}

/** 块间距：一个只含换行符的空行，整行高度压成目标间距 */
private class GapSpan(private val heightPx: Int) : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        v: Int,
        fm: Paint.FontMetricsInt,
    ) {
        fm.top = -heightPx
        fm.ascent = -heightPx
        fm.descent = 0
        fm.bottom = 0
    }
}

/** 首行不缩进、续行缩进（有序列表的悬挂缩进，数字本体是真实文字） */
private class HangingIndentSpan(private val indentPx: Int) : LeadingMarginSpan {
    override fun getLeadingMargin(first: Boolean): Int = if (first) 0 else indentPx

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?,
    ) = Unit
}

/** 引用块：左侧 1dp 竖线 + 间隙，每行都画连成整条（颜色 = outline） */
private class QuoteBarSpan(
    private val barPx: Int,
    private val gapPx: Int,
    private val color: Int,
) : LeadingMarginSpan {
    override fun getLeadingMargin(first: Boolean): Int = barPx + gapPx

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout?,
    ) {
        val oldStyle = p.style
        val oldColor = p.color
        p.style = Paint.Style.FILL
        p.color = color
        val x0 = if (dir > 0) x else x - barPx
        c.drawRect(x0.toFloat(), top.toFloat(), (x0 + barPx).toFloat(), bottom.toFloat(), p)
        p.style = oldStyle
        p.color = oldColor
    }
}

/** 代码块背景：逐行画满宽色块，首行/尾行带圆角（对应 Compose 版的 12dp 圆角盒子） */
private class CodeBgSpan(
    private val color: Int,
    private val radiusPx: Int,
    private val corners: Int,
) : LineBackgroundSpan {
    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineStart: Int,
    ) {
        val oldColor = paint.color
        paint.color = color
        if (corners == CORNER_NONE) {
            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        } else {
            val r = radiusPx.toFloat()
            val radii = FloatArray(8)
            if (corners and CORNER_TOP != 0) {
                radii[0] = r; radii[1] = r; radii[2] = r; radii[3] = r
            }
            if (corners and CORNER_BOTTOM != 0) {
                radii[4] = r; radii[5] = r; radii[6] = r; radii[7] = r
            }
            val path = Path()
            path.addRoundRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), radii, Path.Direction.CW)
            canvas.drawPath(path, paint)
        }
        paint.color = oldColor
    }

    companion object {
        const val CORNER_NONE = 0
        const val CORNER_TOP = 1
        const val CORNER_BOTTOM = 2
    }
}

/** 分割线：整行满宽的 1dp 横线（颜色 = outline） */
private class RuleSpan(private val color: Int) : LineBackgroundSpan {
    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lineStart: Int,
    ) {
        val oldColor = paint.color
        paint.color = color
        canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
        paint.color = oldColor
    }
}


/** 普通文本直接铺一层固定行高；Markdown 走块级构建 */
private fun buildBodySpannable(text: String, markdown: Boolean, m: SpanMetrics): SpannableStringBuilder {
    if (!markdown) {
        val sb = SpannableStringBuilder(text)
        if (sb.isNotEmpty()) {
            sb.setSpan(FixedLineSpan(m.tightLinePx.roundToInt()), 0, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }
    return buildMarkdownSpannable(text, m).first
}

/**
 * 把 Markdown 原文铺成带样式的 Spannable，并返回每个文字块所占区间。
 * 块间距用「空行 + GapSpan」实现，行高用 FixedLineSpan 固定 ——
 * 与 Compose 版的段距/行高一一对应，普通态与选择态永远同一份排版。
 */
private fun buildMarkdownSpannable(
    markdown: String,
    m: SpanMetrics,
): Pair<SpannableStringBuilder, List<TextRange>> {
    val sb = SpannableStringBuilder()
    val ranges = mutableListOf<TextRange>()
    parseMarkdown(markdown).forEachIndexed { index, block ->
        if (index > 0) {
            val topGap = when (block) {
                is MdBlock.Heading -> m.headingTopPx
                is MdBlock.Divider -> m.dividerPadPx
                else -> m.gapPx
            }
            appendGapLine(sb, topGap.roundToInt())
        }
        val start = sb.length
        when (block) {
            is MdBlock.Heading -> {
                appendInlineSpans(sb, block.text, m)
                val big = block.level <= 2
                sb.setSpan(AbsoluteSizeSpan((if (big) m.h1SizePx else m.h2SizePx).roundToInt()), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(StyleSpan(Typeface.BOLD), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(FixedLineSpan((if (big) m.h1LinePx else m.h2LinePx).roundToInt()), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            is MdBlock.Para -> {
                appendInlineSpans(sb, block.text, m)
                sb.setSpan(FixedLineSpan(m.bodyLinePx.roundToInt()), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            is MdBlock.Bullets -> block.items.forEachIndexed { i, item ->
                if (i > 0) appendGapLine(sb, m.itemGapPx.roundToInt())
                val s = sb.length
                appendInlineSpans(sb, item, m)
                // BulletSpan 的缩进 = 直径 + gap，这里凑成 12dp，圆点颜色与正文一致
                val dotPx = m.bulletDotPx.roundToInt()
                sb.setSpan(
                    BulletSpan((m.bulletIndentPx.roundToInt() - dotPx), m.bulletColor, dotPx / 2),
                    s,
                    sb.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                sb.setSpan(FixedLineSpan(m.bodyLinePx.roundToInt()), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            is MdBlock.Numbers -> block.items.forEachIndexed { i, part ->
                if (i > 0) appendGapLine(sb, m.itemGapPx.roundToInt())
                val s = sb.length
                sb.append(part.first).append(".\t")
                appendInlineSpans(sb, part.second, m)
                sb.setSpan(HangingIndentSpan(m.numberIndentPx.roundToInt()), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(TabStopSpan.Standard(m.numberIndentPx.roundToInt()), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(FixedLineSpan(m.bodyLinePx.roundToInt()), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            is MdBlock.Quote -> {
                appendInlineSpans(sb, block.text, m)
                sb.setSpan(QuoteBarSpan(m.quoteBarPx.roundToInt(), m.quoteGapPx.roundToInt(), m.outline), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(ForegroundColorSpan(m.quoteColor), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(FixedLineSpan(m.tightLinePx.roundToInt()), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            is MdBlock.Code -> appendCodeBlock(sb, block.code, m)
            MdBlock.Divider -> appendDivider(sb, m)
        }
        ranges.add(TextRange(start, sb.length))
        // 标题下方额外 6dp、分割线下方 20dp（与 Compose 版的 padding 对应）
        if (block is MdBlock.Heading) appendGapLine(sb, m.headingBottomPx.roundToInt())
        if (block is MdBlock.Divider) appendGapLine(sb, m.dividerPadPx.roundToInt())
    }
    return sb to ranges
}

/** 追加一个"空行"并把它的行高压成 gapPx，形成两块之间的间距 */
private fun appendGapLine(sb: SpannableStringBuilder, gapPx: Int) {
    val s = sb.length
    sb.append('\n')
    sb.append('\n')
    sb.setSpan(GapSpan(gapPx), s + 1, s + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

/** 行内 `**加粗**` 与 `` `代码` ``：去掉标记并落样式（规则与 Compose 版一致） */
private fun appendInlineSpans(sb: SpannableStringBuilder, raw: String, m: SpanMetrics) {
    var i = 0
    while (i < raw.length) {
        when {
            raw.startsWith("**", i) -> {
                val close = raw.indexOf("**", i + 2)
                if (close > i) {
                    val s = sb.length
                    sb.append(raw, i + 2, close)
                    sb.setSpan(StyleSpan(Typeface.BOLD), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = close + 2
                } else {
                    sb.append(raw[i])
                    i++
                }
            }
            raw[i] == '`' -> {
                val close = raw.indexOf('`', i + 1)
                if (close > i + 1) {
                    val s = sb.length
                    sb.append(' ')
                    sb.append(raw, i + 1, close)
                    sb.append(' ')
                    sb.setSpan(TypefaceSpan("monospace"), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(BackgroundColorSpan(m.codeBg), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    sb.setSpan(AbsoluteSizeSpan(m.inlineCodePx.roundToInt()), s, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    i = close + 1
                } else {
                    sb.append(raw[i])
                    i++
                }
            }
            else -> {
                sb.append(raw[i])
                i++
            }
        }
    }
}

/** 代码块：上下内边距空行 + 逐行满宽背景 + 等宽字体，整体是一个 12dp 圆角盒子 */
private fun appendCodeBlock(sb: SpannableStringBuilder, code: String, m: SpanMetrics) {
    // 顶部内边距行（空行，背景从它开始画圆角顶）
    var s = sb.length
    sb.append('\n')
    sb.setSpan(GapSpan(m.codePadPx.roundToInt()), s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.setSpan(CodeBgSpan(m.codeBg, m.codeRadiusPx.roundToInt(), CodeBgSpan.CORNER_TOP), s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    // 代码内容：逐行铺背景（空行也要有，否则色块会漏底）
    val contentStart = sb.length
    val lines = code.split('\n')
    lines.forEachIndexed { li, line ->
        val ls = sb.length
        sb.append(line)
        if (li < lines.lastIndex) sb.append('\n')
        sb.setSpan(CodeBgSpan(m.codeBg, m.codeRadiusPx.roundToInt(), CodeBgSpan.CORNER_NONE), ls, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    val contentEnd = sb.length
    sb.setSpan(TypefaceSpan("monospace"), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.setSpan(AbsoluteSizeSpan(m.codeSizePx.roundToInt()), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.setSpan(FixedLineSpan(m.codeLinePx.roundToInt()), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.setSpan(LeadingMarginSpan.Standard(m.codePadPx.roundToInt()), contentStart, contentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

    // 底部内边距行（圆角底）
    s = sb.length
    sb.append('\n')
    sb.setSpan(GapSpan(m.codePadPx.roundToInt()), s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.setSpan(CodeBgSpan(m.codeBg, m.codeRadiusPx.roundToInt(), CodeBgSpan.CORNER_BOTTOM), s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}

/** 分割线：1dp 满宽横线（上下 20dp 留白由块距补齐） */
private fun appendDivider(sb: SpannableStringBuilder, m: SpanMetrics) {
    val s = sb.length
    sb.append('\n')
    sb.setSpan(GapSpan(m.dividerLinePx.roundToInt()), s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.setSpan(RuleSpan(m.outline), s, s + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
}


/** 长按点落在哪个文字块：返回该块在 Spannable 里的区间（落在块间空白时取下一块） */
private fun blockRangeAt(markdown: String, offset: Int, m: SpanMetrics): TextRange {
    val (_, ranges) = buildMarkdownSpannable(markdown, m)
    if (ranges.isEmpty()) return TextRange(0, 0)
    val index = ranges.indexOfFirst { offset < it.end }
    return ranges[if (index == -1) ranges.lastIndex else index]
}

/* ----------------------------- AI 回答 ----------------------------- */

@Composable
private fun AssistantMessage(
    message: MessageUi,
    sessionTokens: Int,
    readingState: ReadAloudState,
    onToggleReasoning: () -> Unit,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
    onSourcesClick: () -> Unit,
    onBranchPrev: () -> Unit,
    onBranchNext: () -> Unit,
) {
    val colors = DsTheme.colors
    val streaming = message.status == MessageStatus.Streaming || message.status == MessageStatus.Reasoning
    var columnOrigin by remember { mutableStateOf(Offset.Zero) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp)
            .onGloballyPositioned { columnOrigin = it.positionInWindow() },
    ) {
        if (message.reasoning.isNotBlank() || message.status == MessageStatus.Reasoning) {
            ReasoningSection(
                message = message,
                onToggle = onToggleReasoning,
            )
            if (message.text.isNotEmpty()) {
                Spacer(Modifier.height(if (message.reasoningExpanded) 14.dp else 8.dp))
            }
        }

        when (message.status) {
            MessageStatus.Connecting -> TypingDots()
            MessageStatus.Failed -> ErrorTurn(hint = message.errorHint ?: "请检查网络后重试", onRetry = onRetry)
            else -> {
                if (message.text.isNotEmpty()) {
                    // 回答统一用 WebView 渲染：流式/终稿同一个视图，不再原生→网页切换，避免闪一下
                    WebMessageView(
                        markdown = message.text,
                        streaming = streaming,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // 生成完了却没有正文（思考把输出额度吃光）或明确被截断时给一句说明，别让用户对着空白发呆
        val emptyAnswer = message.status == MessageStatus.Completed && message.text.isBlank()
        if (emptyAnswer) {
            Text(
                text = if (message.finishReason == "length") {
                    "回答达到长度上限被截断，可点重新生成再试"
                } else {
                    "这次没有生成正文，可点重新生成再试"
                },
                style = DsText.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
        } else if (message.status == MessageStatus.Completed && message.finishReason == "length") {
            Text(
                text = "回答达到长度上限，已截断",
                style = DsText.caption.copy(fontSize = 12.sp),
                color = colors.textTertiary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (message.status == MessageStatus.Aborted) {
            Text(
                text = "已停止生成",
                style = DsText.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        if (message.status == MessageStatus.Completed || message.status == MessageStatus.Aborted) {
            if (message.sourcesCount > 0) {
                Spacer(Modifier.height(14.dp))
                SourcesPill(count = message.sourcesCount, onClick = onSourcesClick)
            }
            MessageActionBar(
                message = message,
                sessionTokens = sessionTokens,
                readingState = readingState,
                onCopy = onCopy,
                onSpeak = onSpeak,
                onShare = onShare,
                onRegenerate = onRegenerate,
                onBranchPrev = onBranchPrev,
                onBranchNext = onBranchNext,
            )
        }
    }
}

@Composable
private fun ReasoningSection(message: MessageUi, onToggle: () -> Unit) {
    val colors = DsTheme.colors
    val streaming = message.status == MessageStatus.Reasoning
    val headerColor = if (streaming) colors.textPrimary else colors.textSecondary
    val headerText = if (streaming) {
        "正在思考"
    } else {
        "已思考（用时 ${message.reasoningSeconds} 秒）"
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = headerText,
                // 官方思考头：17sp / 行高 26sp / SemiBold(600)
                style = DsText.bodyTight.copy(
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = headerColor,
            )
            Spacer(Modifier.width(8.dp))
            SvgIcon(
                if (message.reasoningExpanded) {
                    R.drawable.ic_chevron_down_outline_20
                } else {
                    R.drawable.ic_chevron_right_outline_20
                },
                tint = colors.textSecondary,
                size = 18.dp,
            )
        }

        if (message.reasoningExpanded && message.reasoning.isNotBlank()) {
            val isDark = colors === DsDarkColors
            // 官方思考区竖线：#2AFFFFFF（深 16% 白）/ #14000000（浅 8% 黑）
            val lineColor = if (isDark) Color(0x2AFFFFFF) else Color(0x14000000)
            Row(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(IntrinsicSize.Min),
            ) {
                // 竖线在内容起点右侧 8dp，上下各内缩 3dp
                Box(
                    Modifier
                        .width(17.dp)
                        .fillMaxHeight(),
                ) {
                    Box(
                        Modifier
                            .padding(start = 8.dp)
                            .width(1.dp)
                            .fillMaxHeight()
                            .padding(vertical = 3.dp)
                            .background(lineColor),
                    )
                }
                // 思考正文：官方独立 markdown 排版，15sp / 行高 24sp / 块间距 6dp
                MessageBodyText(
                    text = message.reasoning,
                    markdown = true,
                    selecting = false,
                    pressWindow = null,
                    color = colors.textSecondary,
                    compact = true,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, bottom = 16.dp),
                )
            }
        }
    }
}

/** 三个点的等待指示（官方 Connecting 态） */
@Composable
fun TypingDots(modifier: Modifier = Modifier) {
    val colors = DsTheme.colors
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = modifier.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 520,
                        delayMillis = index * 140,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.textTertiary.copy(alpha = alpha)),
            )
        }
    }
}

/** 失败态：橘色重试圆钮 + 提示文案（右对齐，与官方一致） */
@Composable
private fun ErrorTurn(hint: String, onRetry: () -> Unit) {
    val colors = DsTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(colors.warning)
                .clickable(role = Role.Button, onClick = onRetry),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "重试",
                tint = Color(0xFF1B1B1B),
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = hint,
            style = DsText.caption.copy(fontSize = 13.sp),
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun SourcesPill(count: Int, onClick: () -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surface2)
            .clickable(role = Role.Button, onClickLabel = "查看来源", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlobeGlyph(color = colors.textSecondary, iconSize = 13.7.dp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$count 个网页",
            style = DsText.caption.copy(fontSize = 13.sp),
            color = colors.textSecondary,
        )
    }
}

/* --------------------------- 动作条 / token --------------------------- */

@Composable
private fun MessageActionBar(
    message: MessageUi,
    sessionTokens: Int,
    readingState: ReadAloudState,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onBranchPrev: () -> Unit,
    onBranchNext: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            // 官方 2.8.0：整行 heightIn(min = 30dp)，不是固定 40dp
            .heightIn(min = 30.dp)
            .offset(x = (-8.5).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (message.branchCount > 1) {
            ActionIcon(
                icon = { tint ->
                    SvgIcon(
                        R.drawable.ic_chevron_left_outline_20_small,
                        tint = tint,
                        size = 20.dp,
                    )
                },
                label = "上一条分支",
                enabled = message.branchIndex > 1,
                onClick = onBranchPrev,
            )
            // 官方是「1」+ 4dp +「/」+ 4dp +「2」三段文本，15sp / 500 / 行高 18sp
            BranchIndexText("${message.branchIndex}")
            Spacer(Modifier.width(4.dp))
            BranchIndexText("/")
            Spacer(Modifier.width(4.dp))
            BranchIndexText("${message.branchCount}")
            ActionIcon(
                icon = { tint ->
                    SvgIcon(
                        R.drawable.ic_chevron_right_outline_20_small,
                        tint = tint,
                        size = 20.dp,
                    )
                },
                label = "下一条分支",
                enabled = message.branchIndex < message.branchCount,
                onClick = onBranchNext,
            )
            Spacer(Modifier.width(10.dp))
        }

        ActionIcon(
            icon = { tint -> SvgIcon(R.drawable.ic_copy_outline, tint = tint, size = 21.dp) },
            label = "复制",
            onClick = onCopy,
        )
        ActionIcon(
            icon = { tint ->
                if (readingState != ReadAloudState.Idle) {
                    // 播放/合成中换成波形条，用品牌蓝，和顶栏的自动朗读状态呼应
                    ReadingBars(state = readingState, color = DsTheme.colors.primary)
                } else {
                    SvgIcon(R.drawable.ic_play_media_outline_20, tint = tint, size = 20.dp)
                }
            },
            label = if (readingState == ReadAloudState.Idle) "朗读" else "停止朗读",
            onClick = onSpeak,
        )
        ActionIcon(
            icon = { tint -> SvgIcon(R.drawable.ic_share_outline_20, tint = tint, size = 20.dp) },
            label = "分享",
            onClick = onShare,
        )

        // token 紧跟分享，与图标同一节奏，组成一组；有分支切换器时用紧凑写法（否则挤到省略号）
        TokenInline(
            message = message,
            sessionTokens = sessionTokens,
            compact = message.branchCount > 1,
        )

        Spacer(Modifier.weight(1f))

        // 重试右缘对齐用户气泡右侧（16dp）
        Box(Modifier.offset(x = 25.dp)) {
            ActionIcon(
                icon = { tint -> SvgIcon(R.drawable.ic_refresh_outline_20, tint = tint, size = 20.dp) },
                label = "重新生成",
                onClick = onRegenerate,
            )
        }
    }
}

@Composable
private fun BranchIndexText(text: String) {
    Text(
        text = text,
        style = DsText.caption.copy(
            fontSize = 15.sp,
            lineHeight = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        color = DsTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ActionIcon(
    icon: @Composable (Color) -> Unit,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Box(
        modifier = Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            )
            // 动作条用的是矢量图（SvgIcon 内部 contentDescription 为 null），这里补语义给读屏和自动化
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        icon(colors.textSecondary)
    }
}

@Composable
private fun TokenLine(message: MessageUi, sessionTokens: Int, estimating: Boolean = false) {
    val colors = DsTheme.colors
    val usage = message.usage
    val prefix = if (estimating) "≈" else ""
    val text = if (usage == null) {
        null
    } else {
        val up = formatTokens(usage.prompt)
        val down = formatTokens(usage.completion)
        val session = formatTokens(sessionTokens)
        "$prefix↑$up ↓$down · 本会话 $session"
    }
    if (text != null) {
        Text(
            text = text,
            style = DsText.meta.copy(fontSize = 11.5.sp),
            color = colors.textSecondary.copy(alpha = if (estimating) 0.6f else 0.9f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.End,
        )
    } else {
        Spacer(Modifier.height(4.dp))
    }
}

internal fun formatTokens(value: Int): String = when {
    value < 1000 -> value.toString()
    value < 10_000 -> String.format("%.1fk", value / 1000f)
    else -> "${value / 1000}k"
}

/** 内容滚到输入栏上方时做一点渐隐，避免文字硬切（官方同款观感） */
@Composable
fun BottomFadeMask(modifier: Modifier = Modifier, height: Int = 28) {
    val colors = DsTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, colors.bg),
                ),
            ),
    )
}



/** token 用量直接排在动作条里（复制之后、朗读之前），字号与图标行匹配 */
@Composable
private fun TokenInline(message: MessageUi, sessionTokens: Int, compact: Boolean = false) {
    val usage = message.usage ?: return
    val colors = DsTheme.colors
    val text = buildString {
        append("↑")
        append(formatTokens(usage.prompt))
        append(" ↓")
        append(formatTokens(usage.completion))
        if (!compact && sessionTokens > 0) {
            append(" tok · 本会话 ")
            append(formatTokens(sessionTokens))
        }
    }
    Box(
        modifier = Modifier
            .height(40.dp)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = DsText.caption.copy(
                fontSize = 13.sp,
                lineHeight = 16.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}






















