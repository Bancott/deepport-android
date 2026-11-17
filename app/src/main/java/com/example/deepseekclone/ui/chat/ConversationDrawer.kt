package com.example.deepseekclone.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
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
            if (state.multiSelect) {
                MultiSelectHeader(onClose = onExitMultiSelect)
            } else {
                Spacer(Modifier.height(8.dp))
                SearchField(onClick = onSearchClick)
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
                DrawerFooter(name = state.userName, onClick = onFooterClick)
            }
        }
    }
}

@Composable
private fun SearchField(onClick: () -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(42.dp)
            .clip(CircleShape)
            .background(colors.surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp),
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
private fun MultiSelectHeader(onClose: () -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "选择对话",
            style = DsText.h2.copy(fontSize = 20.sp),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        CircleIconButton(
            contentDescription = "退出多选",
            onClick = onClose,
            containerColor = colors.surface2,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationList(
    groups: List<ConversationGroup>,
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
                        .padding(start = 22.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = group.label,
                        style = DsText.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    if (group.label == groups.firstOrNull()?.label) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "多选",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.Button, onClick = onToggleMultiSelect),
                        )
                    }
                }
            }
            items(group.items, key = { it.id }) { conversation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .combinedClickable(
                            role = Role.Button,
                            onClick = { onClick(conversation) },
                            onLongClick = { menuTarget = conversation },
                        )
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.title,
                        style = DsText.label,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                val target = menuTarget
                if (target != null && target.id == conversation.id) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { menuTarget = null },
                        containerColor = colors.surface3,
                    ) {
                        ConversationMenuItem("重命名") {
                            menuTarget = null
                            onRename(target)
                        }
                        ConversationMenuItem("置顶") {
                            menuTarget = null
                            onPin(target)
                        }
                        ConversationMenuItem("多选") {
                            menuTarget = null
                            onToggleMultiSelect()
                        }
                        ConversationMenuItem("删除", danger = true) {
                            menuTarget = null
                            onLongPress(target)
                        }
                    }
                }
            }
        }
        item(key = "bottom-space") { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ConversationMenuItem(
    text: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    DropdownMenuItem(
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clickable(role = Role.Checkbox) { onToggleSelect(conversation) }
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, if (isSelected) colors.primary else colors.textTertiary, CircleShape)
                            .background(if (isSelected) colors.primary else Color.Transparent),
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = conversation.title,
                        style = DsText.label,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                .height(56.dp),
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
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
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
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("删除", style = DsText.label, color = tint)
            }
        }
    }
}

@Composable
private fun DrawerFooter(name: String, onClick: () -> Unit) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(role = Role.Button, onClickLabel = "进入设置", onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surface3),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_deepseek_whale),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            style = DsText.label,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.MoreHoriz,
            contentDescription = "设置",
            tint = colors.textSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}




