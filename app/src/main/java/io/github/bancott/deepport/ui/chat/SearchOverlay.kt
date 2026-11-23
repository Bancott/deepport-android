package io.github.bancott.deepport.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.CircleIconButton
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

/**
 * 点搜索框后的展开层：**不做页面跳转**，而是从抽屉宽度连续展开到全屏。
 *
 * 录屏实测（1200×2670，30fps）：
 * - 搜索框左缘 28 → 34 → 51 → 64 → 69，是**同一个框在右移**，不是两个框交叉；
 * - 右缘同时从 387 冲出抽屉右界，说明承载它的容器从 82.5% 展开到了全屏；
 * - 全程约 150–200ms，无整页位移。
 *
 * 所以这里把三个量一起做连续插值（220ms, ease-in-out）：
 * 1. 层宽 `0.825W → W`，左缘固定 0；
 * 2. 搜索框左内边距 `16dp → 46dp`（让出 ‹），右内边距 `26dp → 12dp`；
 * 3. 背景 alpha `0 → 1`，把原会话列表**原地盖住**（就是视频里的“列表淡出”）。
 *
 * 起始帧与抽屉里那颗搜索框像素级重合，所以看不到第二个框，也没有交叉叠影。
 */
@Composable
fun SearchOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    search: (String) -> List<SearchHit>,
    onOpenConversation: (String) -> Unit,
    modifier: Modifier = Modifier,
    drawerFraction: Float = 0.825f,
) {
    val colors = DsTheme.colors
    val easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
    val expand by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(220, easing = easing),
        label = "searchOverlayExpand",
    )
    // 收起动画跑完再退出组合
    if (!visible && expand <= 0.001f) return

    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(visible) {
        if (visible) {
            query = ""
            // 与展开同时起步，只留一帧余量
            kotlinx.coroutines.delay(40)
            runCatching { focus.requestFocus() }
        } else {
            keyboard?.hide()
        }
    }
    BackHandler(enabled = visible) { onClose() }

    // 输入防抖 + 后台线程：loadAll() 每次 runBlocking 查全表
    val hits by produceState(emptyList<SearchHit>(), query, visible) {
        if (!visible || query.isBlank()) {
            value = emptyList()
            return@produceState
        }
        kotlinx.coroutines.delay(140)
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { search(query) }.getOrDefault(emptyList())
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val layerWidth = maxWidth * (drawerFraction + (1f - drawerFraction) * expand)
        // 搜索框左右内边距连续插值：16→46 / 26→12
        val startPad = (16f + 30f * expand).dp
        val endPad = (26f - 14f * expand).dp

        Box(
            Modifier
                .width(layerWidth)
                .fillMaxHeight(),
        ) {
            // 蒙层单独淡入：只盖住下面的会话列表，不影响本层内容的透明度
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = expand }
                    .background(colors.drawerSurface),
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                // 与抽屉同一套槽位：50dp 容器 + 42dp 内容 + 底部对齐，基线固定
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Box(Modifier.fillMaxWidth().height(42.dp)) {
                        // 输入框：左缘跟着展开量右移
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = startPad, end = endPad)
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
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = DsText.label.copy(color = colors.textPrimary),
                                cursorBrush = SolidColor(colors.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focus),
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
                                    onClick = { query = "" },
                                ) {
                                    SvgIcon(
                                        R.drawable.ic_close_outline_16,
                                        tint = colors.textSecondary,
                                        size = 15.dp,
                                    )
                                }
                            }
                        }

                        // ‹ 固定在 8dp 处，随展开量淡入；展开完成后正好落在输入框左侧空档里
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 8.dp)
                                .size(32.dp)
                                .graphicsLayer { alpha = expand * expand },
                            contentAlignment = Alignment.Center,
                        ) {
                            CircleIconButton(
                                contentDescription = "退出搜索",
                                size = 32.dp,
                                onClick = onClose,
                            ) {
                                SvgIcon(
                                    R.drawable.ic_chevron_left_outline_20,
                                    tint = colors.textPrimary,
                                    size = 20.dp,
                                )
                            }
                        }
                    }
                }

                // 结果区随展开量淡入：起始帧保持透明，避免盖不住下面列表时先闪一块空态
                Box(
                    Modifier
                        .weight(1f)
                        .graphicsLayer { alpha = expand },
                ) {
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
                                    onClick = { onOpenConversation(hit.conversationId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
