package com.example.deepseekclone.data

import android.content.Context
import com.example.deepseekclone.ui.chat.AttachmentUi
import com.example.deepseekclone.ui.chat.ChatRole
import com.example.deepseekclone.ui.chat.MessageStatus
import com.example.deepseekclone.ui.chat.MessageUi
import com.example.deepseekclone.ui.chat.SourceItem
import com.example.deepseekclone.ui.chat.TokenUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
 * 会话存储（Room 版）：`conversations` 存元数据与分支选中关系，`messages` 存消息节点，
 * 复杂字段（附件 / 来源）以 JSON 列保存；每次 upsert 只替换这一条会话的消息。
 * 旧版 SharedPreferences 的 JSON 会在首次启动时一次性迁进 Room。
 */
class ConversationStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ds_conversations", Context.MODE_PRIVATE)
    private val dao = ChatDatabase.get(context.applicationContext).chatDao()

    init {
        migrateLegacyIfNeeded()
    }

    /** 全部会话：置顶优先（按置顶时间倒序），其余按更新时间倒序 */
    fun loadAll(): List<StoredConversation> = runBlocking(Dispatchers.IO) {
        val conversations = dao.loadConversations()
        if (conversations.isEmpty()) return@runBlocking emptyList()
        val messages = dao.loadMessages().groupBy { it.conversationId }
        conversations.mapNotNull { conversation ->
            conversation.toStored(messages[conversation.id].orEmpty())
        }
    }

    /** 新建或更新一条会话（只重写这一条会话的消息） */
    fun upsert(conversation: StoredConversation) {
        runBlocking(Dispatchers.IO) {
            dao.replaceConversation(
                conversation.toEntity(),
                conversation.messages.mapIndexed { index, message ->
                    message.toEntity(conversation.id, index)
                },
            )
        }
    }

    fun delete(id: String) {
        runBlocking(Dispatchers.IO) {
            dao.deleteMessages(id)
            dao.deleteConversation(id)
        }
    }

    fun newId(): String = "c-" + UUID.randomUUID().toString().take(8)

    /* ------------------------------ Entity 映射 ------------------------------ */

    private fun StoredConversation.toEntity(): ConversationEntity = ConversationEntity(
        id = id,
        title = title,
        updatedAt = updatedAt,
        pinned = pinned,
        pinnedAt = pinnedAt,
        titleEdited = titleEdited,
        activeChildJson = JSONObject().apply { activeChild.forEach { (k, v) -> put(k, v) } }.toString(),
    )

    private fun ConversationEntity.toStored(messages: List<MessageEntity>): StoredConversation {
        val childMap = mutableMapOf<String, String>()
        runCatching {
            val json = JSONObject(activeChildJson)
            json.keys().forEach { key -> childMap[key] = json.optString(key) }
        }
        return StoredConversation(
            id = id,
            title = title,
            updatedAt = updatedAt,
            messages = messages.sortedBy { it.orderIndex }.map { it.toUi() },
            activeChild = childMap,
            pinned = pinned,
            pinnedAt = pinnedAt,
            titleEdited = titleEdited,
        )
    }

    private fun MessageUi.toEntity(conversationId: String, index: Int): MessageEntity = MessageEntity(
        id = id,
        conversationId = conversationId,
        orderIndex = index,
        parentId = parentId,
        role = if (role == ChatRole.User) "user" else "assistant",
        text = text,
        reasoning = reasoning,
        status = status.name,
        finishReason = finishReason,
        reasoningSeconds = reasoningSeconds,
        reasoningExpanded = reasoningExpanded,
        usagePrompt = usage?.prompt,
        usageCompletion = usage?.completion,
        usageReasoning = usage?.reasoning,
        attachmentsJson = if (attachments.isEmpty()) null else attachmentsToJson(attachments).toString(),
        sourcesJson = if (sources.isEmpty()) null else sourcesToJson(sources).toString(),
        errorHint = errorHint,
    )

    private fun MessageEntity.toUi(): MessageUi {
        val sources = parseSources(sourcesJson?.let { runCatching { JSONArray(it) }.getOrNull() })
        val attachments = parseAttachments(
            attachmentsJson?.let { runCatching { JSONArray(it) }.getOrNull() },
        )
        return MessageUi(
            id = id,
            role = if (role == "user") ChatRole.User else ChatRole.Assistant,
            text = text,
            reasoning = reasoning,
            status = runCatching { MessageStatus.valueOf(status) }
                .getOrDefault(MessageStatus.Completed),
            usage = usagePrompt?.let {
                TokenUsage(
                    prompt = it,
                    completion = usageCompletion ?: 0,
                    reasoning = usageReasoning ?: 0,
                )
            },
            reasoningSeconds = reasoningSeconds,
            reasoningExpanded = reasoningExpanded,
            sourcesCount = sources.size,
            sources = sources,
            errorHint = errorHint,
            finishReason = finishReason,
            parentId = parentId,
            attachments = attachments,
        )
    }

    /* ------------------------------ 旧数据迁移 ------------------------------ */

    private fun migrateLegacyIfNeeded() {
        if (prefs.getBoolean(KEY_ROOM_MIGRATED, false)) return
        val raw = prefs.getString(KEY_LIST, null)
        if (raw.isNullOrBlank()) {
            prefs.edit().putBoolean(KEY_ROOM_MIGRATED, true).apply()
            return
        }
        val imported = runCatching {
            val arr = JSONArray(raw)
            val list = (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { parseLegacy(it) }
            }
            runBlocking(Dispatchers.IO) {
                list.forEach { conversation ->
                    dao.replaceConversation(
                        conversation.toEntity(),
                        conversation.messages.mapIndexed { index, message ->
                            message.toEntity(conversation.id, index)
                        },
                    )
                }
            }
            true
        }.getOrDefault(false)
        if (imported) prefs.edit().putBoolean(KEY_ROOM_MIGRATED, true).apply()
    }

    private fun parseLegacy(o: JSONObject): StoredConversation? {
        val id = o.optString("id")
        if (id.isBlank()) return null
        val rawMessages = parseLegacyMessages(o.optJSONArray("messages"))
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

    private fun parseLegacyMessages(arr: JSONArray?): List<MessageUi> {
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

    /* ------------------------------ JSON 列 ------------------------------ */

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
                    if (item.siteIcon.isNotBlank()) put("siteIcon", item.siteIcon)
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
            list += SourceItem(
                title = title.ifBlank { url },
                url = url,
                snippet = o.optString("snippet"),
                siteIcon = o.optString("siteIcon"),
            )
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
        const val KEY_ROOM_MIGRATED = "room_migrated_v1"
    }
}
