package com.example.deepseekclone.ui.chat

/** 会话列表项（抽屉用） */
data class ConversationUi(
    val id: String,
    val title: String,
    val group: String,
    val pinned: Boolean = false,
)

data class ConversationGroup(
    val label: String,
    val items: List<ConversationUi>,
)

/** 抽屉状态 */
data class DrawerUiState(
    val groups: List<ConversationGroup> = emptyList(),
    val multiSelect: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
    val userName: String = "Li .chu .kun",
    val query: String = "",
) {
    val selectedCount: Int get() = selectedIds.size
}
