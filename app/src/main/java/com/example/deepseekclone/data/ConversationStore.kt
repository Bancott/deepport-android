package com.example.deepseekclone.data

import android.content.Context
import com.example.deepseekclone.ui.chat.AttachmentUi
import com.example.deepseekclone.ui.chat.ChatRole
import com.example.deepseekclone.ui.chat.MessageStatus
import com.example.deepseekclone.ui.chat.MessageUi
import com.example.deepseekclone.ui.chat.SourceItem
import com.example.deepseekclone.ui.chat.TokenUsage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 一条会话。
 *
 * `messages` 是**整棵消息树**的扁平化列表（每个节点带 parentId），
 * `activeChild` 记录"每个父节点当前选中哪个子节点"，两者一起才能还原分支。
 * key 用空串表示根（第一条消息）。
 */
data class StoredConversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<MessageUi> = emptyList(),
    val activeChild: Map<String, String> = emptyMap(),
    val pinned: Boolean = false,
    val pinnedAt: Long = 0L,
    /** 用户手动重命名过，之后不再用首条问题覆盖标题 */
    val titleEdited: Boolean = false,
) {
    /** 抽屉里的分组：今天 / 昨天 / 更早（具体日期） */
    val groupLabel: String
        get() {
            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L
            return when {
                now - updatedAt < day -> "今天"
                now - updatedAt < 2 * day -> "昨天"
                else -> SimpleDateFormat("M月d日", Locale.CHINA).format(Date(updatedAt))
            }
        }

    /** 当前分支上的消息（供搜索 / 预览用） */
    fun activeMessages(): List<MessageUi> {
        if (messages.isEmpty()) return emptyList()
        if (activeChild.isEmpty()) return messages
        val byId = messages.associateBy { it.id }
        val path = mutableListOf<MessageUi>()
        val seen = HashSet<String>()
        var key = ""
        while (true) {
            val childId = activeChild[key] ?: break
            val node = byId[childId] ?: break
            if (!seen.add(childId)) break
            path += node
            key = node.id
        }
        return path.ifEmpty { messages }
    }
}

/**
 * 会话列表存储（Data 层）。目前是 JSON + SharedPreferences，
 * 换成 Room 时只需替换本类实现。
 */
class ConversationStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ds_conversations", Context.MODE_PRIVATE)

    /** 全部会话：置顶优先（按置顶时间倒序），其余按更新时间倒序 */
    fun loadAll(): List<StoredConversation> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                parse(o)
            }
        }.getOrDefault(emptyList())
            .sortedWith(compareByDescending<StoredConversation> { it.pinned }.thenByDescending { it.updatedAt })
    }

    /** 新建或更新一条会话 */
    fun upsert(conversation: StoredConversation) {
        val list = loadAll().filterNot { it.id == conversation.id } + conversation
        write(list)
    }

    fun delete(id: String) {
        write(loadAll().filterNot { it.id == id })
    }

    fun newId(): String = "c-" + UUID.randomUUID().toString().take(8)

    private fun write(list: List<StoredConversation>) {
        val arr = JSONArray()
        list.sortedWith(compareByDescending<StoredConversation> { it.pinned }.thenByDescending { it.updatedAt })
            .forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    /* ------------------------------ JSON ------------------------------ */

    private fun toJson(c: StoredConversation): JSONObject = JSONObject().apply {
        put("id", c.id)
        put("title", c.title)
        put("updatedAt", c.updatedAt)
        put("pinned", c.pinned)
        put("pinnedAt", c.pinnedAt)
        put("titleEdited", c.titleEdited)
        put("messages", messagesToJson(c.messages))
        put("activeChild", JSONObject().apply { c.activeChild.forEach { (k, v) -> put(k, v) } })
    }

    private fun parse(o: JSONObject): StoredConversation? {
        val id = o.optString("id")
        if (id.isBlank()) return null
        val rawMessages = parseMessages(o.optJSONArray("messages"))
        // 老格式没有 parentId：把线性列表串成链，保证树结构自洽
        val messages = if (rawMessages.any { it.parentId != null }) {
            rawMessages
        } else {
            rawMessages.mapIndexed { index, m ->
                m.copy(parentId = rawMessages.getOrNull(index - 1)?.id)
            }
        }
        val activeChild = mutableMapOf<String, String>()
        o.optJSONObject("activeChild")?.let { obj ->
            obj.keys().forEach { k -> activeChild[k] = obj.optString(k) }
        }
        if (activeChild.isEmpty()) {
            // 没存选中关系时，每个父节点选"最后创建"的子节点（即最新分支）
            messages.forEach { m -> activeChild[m.parentId ?: ""] = m.id }
        }
        return StoredConversation(
            id = id,
            title = o.optString("title"),
            updatedAt = o.optLong("updatedAt"),
            messages = messages,
            activeChild = activeChild,
            pinned = o.optBoolean("pinned"),
            pinnedAt = o.optLong("pinnedAt"),
            titleEdited = o.optBoolean("titleEdited"),
        )
    }

    private fun messagesToJson(messages: List<MessageUi>): JSONArray {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("role", if (m.role == ChatRole.User) "user" else "assistant")
                    put("parentId", m.parentId ?: JSONObject.NULL)
                    put("text", m.text)
                    put("reasoning", m.reasoning)
                    put("status", m.status.name)
                    m.finishReason?.let { put("finishReason", it) }
                    put("reasoningSeconds", m.reasoningSeconds)
                    put("reasoningExpanded", m.reasoningExpanded)
                    m.usage?.let {
                        put(
                            "usage",
                            JSONObject()
                                .put("prompt", it.prompt)
                                .put("completion", it.completion)
                                .put("reasoning", it.reasoning),
                        )
                    }
                    if (m.attachments.isNotEmpty()) {
                        put("attachments", attachmentsToJson(m.attachments))
                    }
                    if (m.sources.isNotEmpty()) {
                        put("sources", sourcesToJson(m.sources))
                    }
                },
            )
        }
        return arr
    }

    private fun parseMessages(arr: JSONArray?): List<MessageUi> {
        if (arr == null) return emptyList()
        val list = mutableListOf<MessageUi>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val usage = o.optJSONObject("usage")?.let {
                TokenUsage(
                    prompt = it.optInt("prompt"),
                    completion = it.optInt("completion"),
                    reasoning = it.optInt("reasoning"),
                )
            }
            val sources = parseSources(o.optJSONArray("sources"))
            list += MessageUi(
                id = o.optString("id", "m-$i"),
                role = if (o.optString("role") == "user") ChatRole.User else ChatRole.Assistant,
                text = o.optString("text"),
                reasoning = o.optString("reasoning"),
                status = runCatching { MessageStatus.valueOf(o.optString("status")) }
                    .getOrDefault(MessageStatus.Completed),
                usage = usage,
                reasoningSeconds = o.optInt("reasoningSeconds"),
                reasoningExpanded = o.optBoolean("reasoningExpanded"),
                finishReason = if (o.has("finishReason") && !o.isNull("finishReason")) o.optString("finishReason") else null,
                parentId = if (o.has("parentId") && !o.isNull("parentId")) o.optString("parentId") else null,
                attachments = parseAttachments(o.optJSONArray("attachments")),
                sources = sources,
                sourcesCount = sources.size,
            )
        }
        return list
    }

    private fun attachmentsToJson(list: List<AttachmentUi>): JSONArray {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("name", a.displayName)
                    put("isImage", a.isImage)
                    put("path", a.path ?: JSONObject.NULL)
                    put("size", a.sizeBytes)
                },
            )
        }
        return arr
    }

    private fun sourcesToJson(list: List<SourceItem>): JSONArray {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("url", item.url)
                    put("snippet", item.snippet)
                },
            )
        }
        return arr
    }

    private fun parseSources(arr: JSONArray?): List<SourceItem> {
        if (arr == null) return emptyList()
        val list = mutableListOf<SourceItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val url = o.optString("url")
            val title = o.optString("title")
            if (url.isBlank() && title.isBlank()) continue
            list += SourceItem(title = title.ifBlank { url }, url = url, snippet = o.optString("snippet"))
        }
        return list
    }

    private fun parseAttachments(arr: JSONArray?): List<AttachmentUi> {
        if (arr == null) return emptyList()
        val list = mutableListOf<AttachmentUi>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            list += AttachmentUi(
                id = o.optString("id", "att-$i"),
                displayName = o.optString("name"),
                isImage = o.optBoolean("isImage"),
                path = if (o.has("path") && !o.isNull("path")) o.optString("path") else null,
                sizeBytes = o.optLong("size"),
            )
        }
        return list
    }

    private companion object {
        const val KEY_LIST = "conversations"
    }
}
