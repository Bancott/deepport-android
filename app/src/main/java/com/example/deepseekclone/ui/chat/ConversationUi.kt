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
    /** 充值页登录后抓到的头像地址；空串表示未登录 */
    val avatarUrl: String = "",
    val query: String = "",
    /** 当前正在查看的会话：抽屉里给这一行蓝色高亮 */
    val currentId: String? = null,
) {
    val selectedCount: Int get() = selectedIds.size
}
