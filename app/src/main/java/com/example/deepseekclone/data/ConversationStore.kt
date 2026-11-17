package com.example.deepseekclone.data

import android.content.Context
import com.example.deepseekclone.ui.chat.ChatRole
import com.example.deepseekclone.ui.chat.MessageStatus
import com.example.deepseekclone.ui.chat.MessageUi
import com.example.deepseekclone.ui.chat.TokenUsage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** 一条会话（标题 + 消息 + 时间） */
data class StoredConversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<MessageUi>,
) {
    /** 抽屉里的分组：今天 / 昨天 / 更早 */
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
}

/**
 * 会话列表存储（Data 层）。目前是 JSON + SharedPreferences，
 * 换成 Room 时只需替换本类实现。
 */
class ConversationStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ds_conversations", Context.MODE_PRIVATE)

    fun loadAll(): List<StoredConversation> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                StoredConversation(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    updatedAt = o.optLong("updatedAt"),
                    messages = parseMessages(o.optJSONArray("messages")),
                ).takeIf { it.id.isNotBlank() }
            }
        }.getOrDefault(emptyList()).sortedByDescending { it.updatedAt }
    }

    /** 新建或更新一条会话 */
    fun upsert(conversation: StoredConversation) {
        val list = loadAll().filterNot { it.id == conversation.id } + conversation
        val arr = JSONArray()
        list.sortedByDescending { it.updatedAt }.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("updatedAt", c.updatedAt)
                    put("messages", messagesToJson(c.messages))
                },
            )
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    fun delete(id: String) {
        val list = loadAll().filterNot { it.id == id }
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("title", c.title)
                    put("updatedAt", c.updatedAt)
                    put("messages", messagesToJson(c.messages))
                },
            )
        }
        prefs.edit().putString(KEY_LIST, arr.toString()).apply()
    }

    fun newId(): String = "c-" + UUID.randomUUID().toString().take(8)

    /* ------------------------------ JSON ------------------------------ */

    private fun messagesToJson(messages: List<MessageUi>): JSONArray {
        val arr = JSONArray()
        messages.forEach { m ->
            arr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("role", if (m.role == ChatRole.User) "user" else "assistant")
                    put("text", m.text)
                    put("reasoning", m.reasoning)
                    put("status", m.status.name)
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
            )
        }
        return list
    }

    private companion object {
        const val KEY_LIST = "conversations"
    }
}
