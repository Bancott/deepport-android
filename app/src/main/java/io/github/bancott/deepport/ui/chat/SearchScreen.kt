package io.github.bancott.deepport.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.CircleIconButton
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

/**
 * 会话搜索页：顶栏「返回 + 圆角搜索框」，下面是结果列表。
 * 匹配标题与当前分支上的消息正文，命中处高亮。
 */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    search: (String) -> List<SearchHit>,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    // 进场：搜索栏从左侧短一点的位置，边右移边变长到全宽
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val grow by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(280),
        label = "searchBarGrow",
    )
    val keyboard = LocalSoftwareKeyboardController.current
    // 等转场结束再聚焦：键盘一弹会改变可用高度，转场中重排就是"闪一下"的来源
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(320)
        runCatching { focus.requestFocus() }
    }

    val hits = remember(query) { if (query.isBlank()) emptyList() else search(query) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 8.dp, end = 12.dp)
                // 以左端为锚点：宽度 86%→100%，同时整体右移，看起来像被搜索栏顶开
                .graphicsLayer {
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                    scaleX = 0.86f + 0.14f * grow
                    translationX = -28.dp.toPx() * (1f - grow)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(contentDescription = "返回", onClick = onBack) {
                SvgIcon(
                    R.drawable.ic_chevron_left_outline_20,
                    tint = colors.textPrimary,
                    size = 20.dp,
                )
            }
            Spacer(Modifier.width(6.dp))
            SearchInput(
                query = query,
                onQueryChange = { query = it },
                focusRequester = focus,
                onSearch = { keyboard?.hide() },
                modifier = Modifier.weight(1f),
            )
        }

        when {
            query.isBlank() -> Hint("搜索对话标题或内容")
            hits.isEmpty() -> Hint("未找到相关对话")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                item(key = "count") {
                    Text(
                        text = "${hits.size} 条结果",
                        style = DsText.caption.copy(fontSize = 13.sp),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(start = 20.dp, top = 10.dp, bottom = 6.dp),
                    )
                }
                items(hits, key = { it.conversationId }) { hit ->
                    SearchResultRow(
                        hit = hit,
                        query = query,
                        onClick = {
                            keyboard?.hide()
                            onOpenConversation(hit.conversationId)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(colors.fieldBg)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgIcon(
            R.drawable.ic_search_outline_16,
            tint = colors.textSecondary,
            size = 18.dp,
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = DsText.label.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索对话内容...",
                            style = DsText.label,
                            color = colors.textSecondary,
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            CircleIconButton(
                contentDescription = "清除",
                size = 28.dp,
                onClick = { onQueryChange("") },
            ) {
                SvgIcon(
                    R.drawable.ic_close_outline_16,
                    tint = colors.textSecondary,
                    size = 15.dp,
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    hit: SearchHit,
    query: String,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = highlight(hit.title, query, colors.primary),
            style = DsText.label.copy(fontSize = 16.sp),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (hit.snippet.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = highlight(hit.snippet, query, colors.primary),
                style = DsText.caption.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline),
        )
    }
}

@Composable
private fun Hint(text: String) {
    val colors = DsTheme.colors
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = text,
            style = DsText.body.copy(fontSize = 15.sp),
            color = colors.textTertiary,
            modifier = Modifier.padding(top = 56.dp),
        )
    }
}

/** 把 query 的所有出现高亮（大小写不敏感），其余保持原样 */
private fun highlight(text: String, query: String, color: androidx.compose.ui.graphics.Color): AnnotatedString {
    if (query.isBlank() || text.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        while (true) {
            val index = text.indexOf(query, cursor, ignoreCase = true)
            if (index < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, index))
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) {
                append(text.substring(index, index + query.length))
            }
            cursor = index + query.length
        }
    }
}
