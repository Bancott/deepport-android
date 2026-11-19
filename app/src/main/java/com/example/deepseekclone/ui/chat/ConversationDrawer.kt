package com.example.deepseekclone.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.SvgIcon
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 会话抽屉（82% 屏宽）：
 * - 默认态：搜索框 + 「今天/昨天」分组 + 底部用户行（整块可点，进设置）
 * - 多选态：标题「选择对话」+ 关闭 + 圆形单选 + 底栏置顶/删除
 * - 长按行：重命名 / 置顶 / 多选 / 删除
 */
@Composable
fun ConversationDrawer(
    state: DrawerUiState,
    modifier: Modifier = Modifier,
    onSearchClick: () -> Unit = {},
    onConversationClick: (ConversationUi) -> Unit = {},
    onLongPress: (ConversationUi) -> Unit = {},
    onRename: (ConversationUi) -> Unit = {},
    onPin: (ConversationUi) -> Unit = {},
    onToggleMultiSelect: () -> Unit = {},
    onToggleSelect: (ConversationUi) -> Unit = {},
    onExitMultiSelect: () -> Unit = {},
    onPinSelected: () -> Unit = {},
    onDeleteSelected: () -> Unit = {},
    onFooterClick: () -> Unit = {},
) {
    val colors = DsTheme.colors
    Surface(
        modifier = modifier
            .fillMaxHeight(),
        color = colors.drawerSurface,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                // 底部让出系统手势条高度，避免账户名贴住小白条
                .navigationBarsPadding(),
        ) {
            // 官方 2.8.0：只有头部在「搜索框 ↔ 多选头」之间做交叉淡入 + 尺寸补间（200ms, ease-in-out），
            // 列表与底栏是硬切换，没有整块横向滑动。
            val headerEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
            AnimatedContent(
                targetState = state.multiSelect,
                transitionSpec = {
                    val spec = tween<Float>(200, easing = headerEasing)
                    (fadeIn(spec) togetherWith fadeOut(spec)) using SizeTransform(
                        clip = false,
                        sizeAnimationSpec = { _, _ -> tween(200, easing = headerEasing) },
                    )
                },
                label = "DrawerHeaderTransition",
            ) { multiSelect ->
                Column {
                    if (multiSelect) {
                        MultiSelectHeader(
                            onClose = onExitMultiSelect,
                            selectedCount = state.selectedCount,
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        SearchField(onClick = onSearchClick)
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                if (state.multiSelect) {
                    MultiSelectList(
                        groups = state.groups,
                        selected = state.selectedIds,
                        onToggleSelect = onToggleSelect,
                    )
                } else {
                    ConversationList(
                        groups = state.groups,
                        currentId = state.currentId,
                        onClick = onConversationClick,
                        onLongPress = onLongPress,
                        onPin = onPin,
                        onRename = onRename,
                        onToggleMultiSelect = onToggleMultiSelect,
                    )
                }
            }

            if (state.multiSelect) {
                MultiSelectBar(
                    enabled = state.selectedCount > 0,
                    onPin = onPinSelected,
                    onDelete = onDeleteSelected,
                )
            } else {
                DrawerFooter(name = state.userName, avatarUrl = state.avatarUrl, onClick = onFooterClick)
            }
        }
    }
}

/**
 * 右边界全部取自官方截图实测（屏幕 400dp、抽屉 330dp）：
 * - 搜索框右缘 304dp，右侧留 26dp
 * - 历史高亮行右缘 310dp，留 20dp
 * - 行内三点墨迹右缘 294dp（在高亮行内再缩 16dp）
 * - 底栏三点墨迹右缘 302dp
 * - 分组「调节」图标墨迹右缘 294.7dp（官方本身就比搜索框内缩）
 */
private val SearchEnd = 26.dp
private val RowEnd = 20.dp
private val FooterDotsEnd = 28.dp
private val TuneEnd = 33.dp

/**
 * 会话行几何（官方 2.8.0 ChatSessionItem.kt:167-180 / sz6.java:1101,1141）：
 * - 行盒最小高 **44dp**（不是 52dp）
 * - 行高亮圆角 **16dp**（不是 12dp）
 * - 行内容左右内边距 **16dp**
 * - 标题 **16sp / lineHeight 24sp / letterSpacing 0**，当前会话 500、其余 400
 */
private val RowHeight = 44.dp
private val RowCorner = 16.dp
private val RowInset = 16.dp
private val RowTitleSize = 16.sp
private val RowTitleLineHeight = 24.sp

/**
 * 官方三点：点径 3.3dp、圆心间距 6.7dp。
 * Material 的 MoreHoriz 点更大、间距只有 5.5dp 左右，所以这里自绘。
 */
@Composable
private fun ThreeDots(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.4.dp,
    gap: Dp = 3.3.dp,
) {
    Canvas(
        modifier.size(width = dotSize * 3 + gap * 2, height = dotSize),
    ) {
        val d = dotSize.toPx()
        val step = d + gap.toPx()
        val r = d / 2f
        repeat(3) { i ->
            drawCircle(color, r, androidx.compose.ui.geometry.Offset(r + step * i, size.height / 2f))
        }
    }
}

@Composable
private fun SearchField(onClick: () -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = SearchEnd)
            .height(42.dp)
            .clip(CircleShape)
            .background(colors.fieldBg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgIcon(
            R.drawable.ic_search_outline_16,
            tint = colors.textSecondary,
            size = 18.dp,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "搜索对话内容...",
            style = DsText.label,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MultiSelectHeader(onClose: () -> Unit, selectedCount: Int) {
    val colors = DsTheme.colors
    // 官方：头高 48dp，标题 17sp / 500；有选中时文案是「已选择 N 个对话」
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (selectedCount > 0) "已选择 $selectedCount 个对话" else "选择对话",
            style = DsText.h2.copy(
                fontSize = 17.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        CircleIconButton(
            contentDescription = "退出多选",
            onClick = onClose,
            containerColor = colors.surface2,
        ) {
            SvgIcon(
                R.drawable.ic_close_outline_20,
                tint = colors.textPrimary,
                size = 20.dp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationList(
    groups: List<ConversationGroup>,
    currentId: String?,
    onClick: (ConversationUi) -> Unit,
    onLongPress: (ConversationUi) -> Unit,
    onPin: (ConversationUi) -> Unit,
    onRename: (ConversationUi) -> Unit,
    onToggleMultiSelect: () -> Unit,
) {
    val colors = DsTheme.colors
    var menuTarget by remember { mutableStateOf<ConversationUi?>(null) }

    LazyColumn(Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "h-${group.label}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = TuneEnd, top = 18.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = group.label,
                        style = DsText.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (group.label == groups.firstOrNull()?.label) {
                        SvgIcon(
                            R.drawable.ic_checklist_outline_16,
                            tint = colors.textSecondary,
                            size = 20.dp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = onToggleMultiSelect),
                        )
                    }
                }
            }
            items(group.items, key = { it.id }) { conversation ->
                val isCurrent = conversation.id == currentId
                // 长按/点三点时给该行一个灰底；已经是蓝色选中态则不叠灰
                val isMenuTarget = menuTarget?.id == conversation.id
                val density = LocalDensity.current
                var rowTopPx by remember { mutableStateOf(0f) }
                var rowWidthPx by remember { mutableStateOf(0f) }
                // 在"点击那一刻"算朝向：组合阶段读 rowTopPx 往往还是 0，会永远判成向下
                var openOffsetY by remember { mutableStateOf(0.dp) }
                fun resolveOffsetY() {
                    openOffsetY = with(density) {
                        if (rowTopPx.toDp() > 212.dp) (-212).dp else 0.dp
                    }
                }
                // 右边缘对齐高亮行右缘（官方菜单卡片与行右缘同为 310dp）
                val menuOffsetX = with(density) {
                    rowWidthPx.toDp() - MenuWidth - RowEnd
                }
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .onGloballyPositioned {
                                rowTopPx = it.positionInRoot().y
                                rowWidthPx = it.size.width.toFloat()
                            }
                            .padding(start = 10.dp, end = RowEnd)
                            // 不用 clip：行尾三点要略微越过圆角背景，才能和底部账户行的三点
                            // 落在同一条竖线上；圆角交给 background 的 shape 自己画
                            .background(
                                color = when {
                                    isCurrent -> colors.primary.copy(alpha = 0.12f)
                                    isMenuTarget -> colors.surface2
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(RowCorner),
                            )
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = { onClick(conversation) },
                                onLongClick = { resolveOffsetY(); menuTarget = conversation },
                            )
                            .padding(horizontal = RowInset),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (conversation.pinned) {
                            SvgIcon(
                                R.drawable.ic_pin_outline_20,
                                tint = if (isCurrent) colors.primary else colors.textTertiary,
                                size = 14.dp,
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = conversation.title,
                            style = DsText.label.copy(
                                fontSize = RowTitleSize,
                                lineHeight = RowTitleLineHeight,
                                letterSpacing = 0.sp,
                                // 官方：当前会话 500，其余 400（sz6.java:971 / 1197）
                                fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
                            ),
                            color = if (isCurrent) colors.primary else colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        // 三点：点它和长按一样进二级菜单
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button) { resolveOffsetY(); menuTarget = conversation },
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            ThreeDots(color = if (isCurrent) colors.primary else colors.textTertiary)
                        }
                    }

                    val target = menuTarget
                    if (target != null && target.id == conversation.id) {
                        ConversationMenu(
                            target = target,
                            offsetY = openOffsetY,
                            offsetX = menuOffsetX,
                            onDismiss = { menuTarget = null },
                            onRename = onRename,
                            onPin = onPin,
                            onToggleMultiSelect = onToggleMultiSelect,
                            onDelete = onLongPress,
                        )
                    }
                }
            }
        }
        item(key = "bottom-space") { Spacer(Modifier.height(12.dp)) }
    }
}

private val MenuWidth = 172.dp

/** 会话二级菜单：默认往上弹，上方空间不足时由 DropdownMenu 自己回落到下方 */
@Composable
private fun ConversationMenu(
    target: ConversationUi,
    offsetY: Dp,
    offsetX: Dp,
    onDismiss: () -> Unit,
    onRename: (ConversationUi) -> Unit,
    onPin: (ConversationUi) -> Unit,
    onToggleMultiSelect: () -> Unit,
    onDelete: (ConversationUi) -> Unit,
) {
    val colors = DsTheme.colors
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(MenuWidth),
        shape = RoundedCornerShape(16.dp),
        containerColor = colors.surface3,
        // 官方是从这一行往上弹；offsetY 由行位置决定，顶部几行改朝下，
        // offsetX 让菜单右边缘对齐条目右边缘
        offset = DpOffset(offsetX, offsetY),
    ) {
        ConversationMenuItem("重命名", R.drawable.ic_edit_outline_20) { onDismiss(); onRename(target) }
        ConversationMenuItem(
            if (target.pinned) "取消置顶" else "置顶",
            if (target.pinned) R.drawable.ic_unpin_outline_20 else R.drawable.ic_pin_outline_20,
        ) { onDismiss(); onPin(target) }
        ConversationMenuItem("多选", R.drawable.ic_checklist_outline_16) { onDismiss(); onToggleMultiSelect() }
        // 功能项与危险项之间用一条细线隔开（官方就是这样分组的）
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            thickness = 0.6.dp,
            color = colors.outline,
        )
        ConversationMenuItem("删除", R.drawable.ic_trash_outline_20, danger = true) { onDismiss(); onDelete(target) }
    }
}

@Composable
private fun ConversationMenuItem(
    text: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    DropdownMenuItem(
        leadingIcon = {
            SvgIcon(
                iconRes,
                tint = if (danger) colors.danger else colors.textSecondary,
                size = 18.dp,
            )
        },
        text = {
            Text(
                text = text,
                style = DsText.label,
                color = if (danger) colors.danger else colors.textPrimary,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        },
        onClick = onClick,
    )
}

@Composable
private fun MultiSelectList(
    groups: List<ConversationGroup>,
    selected: Set<String>,
    onToggleSelect: (ConversationUi) -> Unit,
) {
    val colors = DsTheme.colors
    LazyColumn(Modifier.fillMaxSize()) {
        groups.forEach { group ->
            item(key = "mh-${group.label}") {
                Text(
                    text = group.label,
                    style = DsText.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 22.dp, top = 18.dp, bottom = 6.dp),
                )
            }
            items(group.items, key = { "m-${it.id}" }) { conversation ->
                val isSelected = conversation.id in selected
                // 官方 2.8.0：勾选框 alpha 0→1、标题右移 0→30dp，均 200ms ease-in-out，无逐项延迟
                val rowEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
                val checkAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = tween(200, easing = rowEasing),
                    label = "checkboxAlpha",
                )
                val titleShift by animateFloatAsState(
                    targetValue = if (isSelected) 30f else 0f,
                    animationSpec = tween(200, easing = rowEasing),
                    label = "titleShift",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(RowHeight)
                        .padding(horizontal = RowInset)
                        .background(
                            color = if (isSelected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                            shape = RoundedCornerShape(RowCorner),
                        )
                        .clickable(role = Role.Checkbox) {
                            onToggleSelect(conversation)
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .alpha(checkAlpha)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) colors.primary else colors.textTertiary, CircleShape)
                            .background(if (isSelected) colors.primary else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            SvgIcon(
                                R.drawable.ic_checked_filled_16,
                                tint = colors.onPrimary,
                                size = 12.dp,
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = conversation.title,
                        style = DsText.label.copy(
                            fontSize = RowTitleSize,
                            lineHeight = RowTitleLineHeight,
                            letterSpacing = 0.sp,
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.offset(x = titleShift.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiSelectBar(
    enabled: Boolean,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = DsTheme.colors
    val tint = if (enabled) colors.textPrimary else colors.textTertiary
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = enabled, onClick = onPin),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SvgIcon(
                    R.drawable.ic_pin_outline_20,
                    tint = tint,
                    size = 20.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text("置顶", style = DsText.label, color = tint)
            }
            Box(
                Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(colors.outline),
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = enabled, onClick = onDelete),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SvgIcon(
                    R.drawable.ic_trash_outline_20,
                    tint = tint,
                    size = 20.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text("删除", style = DsText.label, color = tint)
            }
        }
    }
}

@Composable
private fun DrawerFooter(name: String, avatarUrl: String, onClick: () -> Unit) {
    val colors = DsTheme.colors
    val loggedIn = avatarUrl.isNotBlank() || name.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(role = Role.Button, onClickLabel = "进入设置", onClick = onClick)
            .padding(start = 16.dp, end = FooterDotsEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (loggedIn) colors.surface3 else colors.outline.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl.isNotBlank()) {
                RemoteAvatar(avatarUrl, 40.dp)
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_deepseek_whale),
                    contentDescription = null,
                    tint = if (loggedIn) colors.primary else colors.textTertiary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (name.isBlank()) "未登录" else name,
            style = DsText.label,
            color = if (loggedIn) colors.textPrimary else colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ThreeDots(color = colors.textSecondary)
    }
}


/** 极简网络头像：不引图片库，直接拉一次位图并缓存 */
@Composable
private fun RemoteAvatar(url: String, size: androidx.compose.ui.unit.Dp) {
    var bmp by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(url) {
        bmp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                c.connectTimeout = 4000
                c.readTimeout = 4000
                android.graphics.BitmapFactory.decodeStream(c.inputStream)
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
    val b = bmp
    if (b != null) {
        androidx.compose.foundation.Image(
            bitmap = b,
            contentDescription = null,
            modifier = Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
    }
}
