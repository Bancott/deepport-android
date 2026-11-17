package com.example.deepseekclone.data

import android.content.Context
import com.example.deepseekclone.ui.chat.ChatRole
import com.example.deepseekclone.ui.chat.MessageStatus
import com.example.deepseekclone.ui.chat.MessageUi
import com.example.deepseekclone.ui.chat.TokenUsage
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地会话存储（Data 层）。
 * 先把"当前会话 + 消息列表"落盘，杀掉进程重进还能看到上次的对话。
 * 后续换 Room 时只替换这个类的实现，ViewModel 不用改。
 */
class ChatStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ds_chat", Context.MODE_PRIVATE)

    fun save(title: String, messages: List<MessageUi>) {
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
        val root = JSONObject()
            .put("title", title)
            .put("messages", arr)
        prefs.edit().putString(KEY_CURRENT, root.toString()).apply()
    }

    fun load(): Pair<String, List<MessageUi>>? {
        val raw = prefs.getString(KEY_CURRENT, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)
            val arr = root.optJSONArray("messages") ?: JSONArray()
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
            root.optString("title") to list
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(KEY_CURRENT).apply()
    }

    private companion object {
        const val KEY_CURRENT = "current_conversation"
    }
}
