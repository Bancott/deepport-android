package io.github.bancott.deepport.ui.chat

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
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
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.CircleIconButton
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsDarkColors
import io.github.bancott.deepport.ui.theme.DsTheme

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
            // 官方 2.8.0：头部在「搜索框 ↔ 多选头」之间做交叉淡入 + 尺寸补间（200ms, ease-in-out）；
            // 列表是同一个，行内 checkboxAlpha/titleShift 两个 200ms 动画随多选模式整体切换。
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
                // 官方 2.8.0（ChatSessionItem.kt:180）：普通/多选是**同一个列表**，
                // 行内只做 checkboxAlpha（0→1）与 titleShift（0→30dp）两个 200ms 动画，
                // 驱动布尔是"是否处于多选模式"——进模式瞬间所有行同时让位，不是逐行按选中态切换。
                ConversationList(
                    groups = state.groups,
                    currentId = state.currentId,
                    multiSelect = state.multiSelect,
                    selected = state.selectedIds,
                    onClick = onConversationClick,
                    onToggleSelect = onToggleSelect,
                    onLongPress = onLongPress,
                    onPin = onPin,
                    onRename = onRename,
                    onToggleMultiSelect = onToggleMultiSelect,
                )
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
    // 与普通头（8 + 42 = 50dp）同高：切多选时列表不会上下移动，
    // 标题才能表现为纯水平位移
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
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
    multiSelect: Boolean,
    selected: Set<String>,
    onClick: (ConversationUi) -> Unit,
    onToggleSelect: (ConversationUi) -> Unit,
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
                    if (!multiSelect && group.label == groups.firstOrNull()?.label) {
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
                val isSelected = conversation.id in selected
                // 长按/点三点时给该行一个灰底；已经是蓝色选中态则不叠灰
                val isMenuTarget = menuTarget?.id == conversation.id
                val density = LocalDensity.current
                // 行在窗口中的矩形：菜单定位完全以它为准（官方不跟手指）
                var rowRect by remember { mutableStateOf(Rect.Zero) }
                // 官方 2.8.0（ChatSessionItem.kt:180）：动画标签就叫 checkboxAlpha / titleShift，
                // 均 200ms ease-in-out；驱动布尔是"多选模式"——进模式瞬间全体行勾选框淡入、标题统一右移 30dp。
                // 勾选框只用填充色区分选中/未选中，不再各自进出。
                val rowEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
                val checkAlpha by animateFloatAsState(
                    targetValue = if (multiSelect) 1f else 0f,
                    animationSpec = tween(200, easing = rowEasing),
                    label = "checkboxAlpha",
                )
                val titleShift by animateFloatAsState(
                    targetValue = if (multiSelect) 30f else 0f,
                    animationSpec = tween(200, easing = rowEasing),
                    label = "titleShift",
                )
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(RowHeight)
                            .padding(start = 10.dp, end = RowEnd)
                            // 锚点取「高亮行」的矩形（padding 之后），菜单右缘才能对齐行的右缘；
                            // 之前挂在 padding 之前，会多出一个 RowEnd 的偏移，菜单整体偏右。
                            .onGloballyPositioned {
                                rowRect = it.boundsInWindow()
                            }
                            // 不用 clip：行尾三点要略微越过圆角背景，才能和底部账户行的三点
                            // 落在同一条竖线上；圆角交给 background 的 shape 自己画
                            .background(
                                color = when {
                                    multiSelect && isSelected -> colors.primary.copy(alpha = 0.12f)
                                    !multiSelect && isCurrent -> colors.primary.copy(alpha = 0.12f)
                                    isMenuTarget -> colors.surface2
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(RowCorner),
                            )
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = {
                                    if (multiSelect) onToggleSelect(conversation) else onClick(conversation)
                                },
                                onLongClick = { if (!multiSelect) menuTarget = conversation },
                            )
                            .padding(horizontal = RowInset),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        // 勾选框：叠加在内容起点，空间由 titleShift 30dp（=框 20 + 间距 10）让出，
                        // 不占布局流——普通模式下标题仍从原位起排，不会横向跳位
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .alpha(checkAlpha)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    if (isSelected) colors.primary else colors.textTertiary,
                                    CircleShape,
                                )
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    // 头部已同高，这里只用绘制期水平位移，避免任何布局参与
                                    .offset(x = titleShift.dp)
                                    .weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (conversation.pinned) {
                                    SvgIcon(
                                        R.drawable.ic_pin_outline_20,
                                        tint = if (isCurrent && !multiSelect) colors.primary else colors.textTertiary,
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
                                        fontWeight = if (isCurrent && !multiSelect) FontWeight.Medium else FontWeight.Normal,
                                    ),
                                    color = if (isCurrent && !multiSelect) colors.primary else colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            // 三点只在当前会话显示；其他会话的菜单走长按，
                            // 底部账户行的三点保持常驻（不在这一段里）。
                            if (isCurrent && !multiSelect) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            role = Role.Button,
                                        ) { menuTarget = conversation },
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    ThreeDots(color = colors.primary)
                                }
                            }
                        }
                    }

                    val target = menuTarget
                    if (!multiSelect && target != null && target.id == conversation.id) {
                        SessionMenu(
                            target = target,
                            anchor = rowRect,
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

/** 官方 ContextMenuDefaults：固定宽 208dp、圆角 16dp、项高 48dp、图标 20dp、图标–文字 12dp */
private val MenuWidth = 208.dp
private val MenuCorner = 16.dp
private val MenuItemHeight = 48.dp
private val MenuIconSize = 20.dp
private val MenuIconGap = 12.dp

/**
 * 官方会话菜单的定位规则（PopupPositionProvider）：
 * - 锚点是**被长按/点三点的行**，与手指位置无关；
 * - 默认贴行上方（菜单底 = 行顶 − 8dp 间隙），要求「行顶 − 菜单高 ≥ 180px」，否则贴行下方；
 * - LTR 下菜单右缘对齐行右缘，再 clamp 进窗口；
 * - 越界先 clamp y。
 */
private class SessionMenuPositionProvider(
    private val anchor: Rect,
    private val gapPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val ltr = layoutDirection == LayoutDirection.Ltr
        var x = if (ltr) anchor.right.toInt() - popupContentSize.width else anchor.left.toInt()
        x = x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val above = anchor.top.toInt() - popupContentSize.height >= 180
        var y = if (above) {
            anchor.top.toInt() - popupContentSize.height - gapPx
        } else {
            anchor.bottom.toInt() + gapPx
        }
        y = y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        return IntOffset(x, y)
    }
}

/**
 * 会话二级菜单：自绘 Popup。
 * 进入 scale 0.8→1（spring 0.75 / 3000）；**关闭不做动画**——点外部/菜单项立即消失，
 * 避免"缩一下再消失"的顿挫感（用户实测要求）。
 */
@Composable
private fun SessionMenu(
    target: ConversationUi,
    anchor: Rect,
    onDismiss: () -> Unit,
    onRename: (ConversationUi) -> Unit,
    onPin: (ConversationUi) -> Unit,
    onToggleMultiSelect: () -> Unit,
    onDelete: (ConversationUi) -> Unit,
) {
    val colors = DsTheme.colors
    var appeared by remember { mutableStateOf(false) }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 3000f),
        label = "sessionMenuScale",
    )
    val above = anchor.top.toInt() - menuSize.height >= 180
    val origin = when {
        !above -> TransformOrigin(0.5f, 0f)
        anchor.width >= menuSize.width -> TransformOrigin(0.5f, 1f)
        else -> TransformOrigin(1f, 1f)
    }
    // 官方菜单与行之间约 8dp 间隙（会话设置.jpg 实测 8.3dp）
    val gapPx = with(LocalDensity.current) { 8.dp.toPx().toInt() }
    Popup(
        popupPositionProvider = SessionMenuPositionProvider(anchor, gapPx),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier
                .width(MenuWidth)
                .onSizeChanged { menuSize = it }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = origin
                },
            shape = RoundedCornerShape(MenuCorner),
            color = colors.surface3,
            // 官方是轻阴影（深色下几乎不可见的落影）
            shadowElevation = 8.dp,
        ) {
            // 实测菜单无竖向内边距：4×48dp 项 + 1dp 分隔线 = 193dp（项目此前多加了上下 16dp）
            Column {
                SessionMenuItem("重命名", R.drawable.ic_edit_outline_20) { onDismiss(); onRename(target) }
                SessionMenuDivider()
                SessionMenuItem(
                    if (target.pinned) "取消置顶" else "置顶",
                    if (target.pinned) R.drawable.ic_unpin_outline_20 else R.drawable.ic_pin_outline_20,
                ) { onDismiss(); onPin(target) }
                SessionMenuDivider()
                SessionMenuItem("多选", R.drawable.ic_checklist_outline_16) {
                    onDismiss()
                    onToggleMultiSelect()
                }
                SessionMenuDivider()
                SessionMenuItem("删除", R.drawable.ic_trash_outline_20, danger = true) {
                    onDismiss()
                    onDelete(target)
                }
            }
        }
    }
}

@Composable
private fun SessionMenuDivider() {
    val colors = DsTheme.colors
    // 深色主题的 outline 与 surface3 同为 #333333，直接用它画线等于没有；
    // 这里按「文字色 8%/6% 叠一层」画一条真正的细线。
    val alpha = if (colors === DsDarkColors) 0.08f else 0.06f
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.textPrimary.copy(alpha = alpha)),
    )
}

@Composable
private fun SessionMenuItem(
    text: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MenuItemHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgIcon(
            iconRes,
            tint = if (danger) colors.danger else colors.textSecondary,
            size = MenuIconSize,
        )
        Spacer(Modifier.width(MenuIconGap))
        Text(
            text = text,
            style = DsText.body.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.sp,
            ),
            color = if (danger) colors.danger else colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
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
