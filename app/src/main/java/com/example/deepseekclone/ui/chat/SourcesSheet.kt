package com.example.deepseekclone.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.common.GlobeGlyph
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 来源列表 Sheet：点回答下方的「N 个网页」弹出，
 * 标题 + 可滚动列表（站点名 / 标题 / 摘要），点条目用浏览器打开。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesSheet(
    sources: List<SourceItem>,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val colors = DsTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface2,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlobeGlyph(color = colors.textSecondary, iconSize = 14.6.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${sources.size} 个网页",
                    style = DsText.label.copy(fontSize = 16.sp),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "关闭",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.outline),
            )
            if (sources.isEmpty()) {
                Text(
                    text = "这条回答没有记录来源明细",
                    style = DsText.caption.copy(fontSize = 13.sp),
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                )
                return@Column
            }
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(sources, key = { index, item -> "$index-${item.url}" }) { _, item ->
                    SourceRow(
                        source = item,
                        onClick = {
                            onDismiss()
                            onOpen(item.url)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceRow(
    source: SourceItem,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface3)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 站点徽标：取域名首字母 + 按域名散列取色，离线也有稳定图标
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(siteColor(source.host)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = source.host.take(1).uppercase(),
                    style = DsText.meta.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = source.host,
                style = DsText.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = source.title,
            style = DsText.bodyTight.copy(fontSize = 15.sp, lineHeight = 21.sp),
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (source.snippet.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = source.snippet,
                style = DsText.caption.copy(fontSize = 12.sp, lineHeight = 18.sp),
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 按域名散列出一组固定颜色，保证同一站点每次都是同一个徽标色 */
private fun siteColor(host: String): Color {
    val palette = listOf(
        Color(0xFF4C6EF6),
        Color(0xFF12A150),
        Color(0xFFE06560),
        Color(0xFF9A5CF5),
        Color(0xFFE08A1E),
        Color(0xFF1E9BB5),
    )
    if (host.isEmpty()) return palette[0]
    val hash = host.fold(0) { acc, ch -> (acc * 31 + ch.code) and 0x7fffffff }
    return palette[hash % palette.size]
}
