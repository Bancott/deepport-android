package com.example.deepseekclone.ui.chat

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
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

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
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

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
                .padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(contentDescription = "返回", onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
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
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
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
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(15.dp),
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
