package com.example.deepseekclone.ui.chat

/** 输入区两种模式：文字输入 / 按住说话 */
enum class InputMode { Text, Voice }

/**
 * 流式状态机。UI 必须覆盖全部 7 个状态。
 * Idle -> Connecting -> Reasoning -> Streaming -> Completed
 *                          \-> Aborted / Failed
 */
enum class GenerationState {
    Idle,
    Connecting,
    Reasoning,
    Streaming,
    Completed,
    Aborted,
    Failed,
}

enum class ChatRole { User, Assistant }

/** 单条消息的渲染状态 */
enum class MessageStatus {
    Sent,
    Connecting,
    Reasoning,
    Streaming,
    Completed,
    Aborted,
    Failed,
}

/**
 * 一次回答的 token 用量。
 * prompt = 输入；completion = 输出（含思考）；reasoning 为其中思考部分。
 */
data class TokenUsage(
    val prompt: Int = 0,
    val completion: Int = 0,
    val reasoning: Int = 0,
) {
    val total: Int get() = prompt + completion
}

/** 联网搜索来源（回答下方「N 个网页」展开的内容） */
data class SourceItem(
    val title: String,
    val url: String,
    val snippet: String = "",
) {
    /** 用于列表上显示的站点名 */
    val host: String
        get() = runCatching { java.net.URI(url).host.orEmpty().removePrefix("www.") }
            .getOrDefault("")
            .ifBlank { url.take(32) }
}

/**
 * 消息模型。会话是一棵树，`id` 稳定、`parentId` 指父节点；
 * 分支切换只影响"父节点选了哪个子节点"，由 ViewModel 算好 active path 再传下来。
 *
 * branchIndex / branchCount 是**派生字段**（由 ViewModel 每次重算 active path 时填），
 * 数据层不落盘，避免切换分支后一堆节点要同时改。
 */
data class MessageUi(
    val id: String,
    val role: ChatRole,
    val text: String = "",
    val reasoning: String = "",
    val status: MessageStatus = MessageStatus.Completed,
    val usage: TokenUsage? = null,
    /** "已思考（用时 N 秒）" 的 N */
    val reasoningSeconds: Int = 0,
    val reasoningExpanded: Boolean = false,
    /** 联网搜索来源数量，0 表示不显示来源 pill */
    val sourcesCount: Int = 0,
    /** 来源明细（接口有返回才填） */
    val sources: List<SourceItem> = emptyList(),
    /** 分支切换器：第几条 / 共几条（派生，不持久化） */
    val branchIndex: Int = 1,
    val branchCount: Int = 1,
    /** 失败原因，如"请检查网络后重试" */
    val errorHint: String? = null,
    /** 接口返回的 finish_reason：length 表示被输出额度截断，stop 表示正常结束 */
    val finishReason: String? = null,
    /** 父节点 id；null 表示会话第一条消息 */
    val parentId: String? = null,
    /** 本条消息携带的附件（用户消息才有） */
    val attachments: List<AttachmentUi> = emptyList(),
)

/** 附件（待发送 / 已发送都复用） */
data class AttachmentUi(
    val id: String,
    val displayName: String,
    val isImage: Boolean = false,
    /** 已复制到 App 私有目录的本地绝对路径；null 表示只有名字（历史数据） */
    val path: String? = null,
    /** 非图片文件的可读字节数，用于 chip 上显示大小 */
    val sizeBytes: Long = 0L,
    /** null 表示未开始上传；0f..1f 为进度 */
    val progress: Float? = null,
) {
    val sizeLabel: String
        get() = when {
            sizeBytes <= 0L -> ""
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format(java.util.Locale.CHINA, "%.1f MB", sizeBytes / 1048576f)
        }
}

/** 输入卡状态 */
data class InputUiState(
    val text: String = "",
    val mode: InputMode = InputMode.Text,
    val deepThinking: Boolean = true,
    val smartSearch: Boolean = true,
    val attachments: List<AttachmentUi> = emptyList(),
    /** 语音转写中 */
    val transcribing: Boolean = false,
    /** 转写回填后的提示条文案，非空时展示 */
    val transcribedNotice: String? = null,
) {
    val canSend: Boolean
        get() = !transcribing && (text.isNotBlank() || attachments.isNotEmpty())
}

/** 朗读状态：Idle 未朗读 / Loading 请求合成 / Playing 出声中 */
enum class ReadAloudState { Idle, Loading, Playing }

/** 聊天页整体 UiState（后续接入 ViewModel 时只暴露这一个对象） */
data class ChatUiState(
    /** 空会话时官方不显示标题，第一条消息发出后才是"新对话" */
    val title: String = "",
    val greeting: String = "嗨！今天想聊些什么？",
    val generation: GenerationState = GenerationState.Idle,
    val input: InputUiState = InputUiState(),
    val messages: List<MessageUi> = emptyList(),
    /** 当前 active path 上的累计 token */
    val sessionTokens: Int = 0,
    val isSpeechOutputOn: Boolean = false,
    /** 正在朗读（或准备朗读）的消息 id；null 表示没有 */
    val readingMessageId: String? = null,
    val readingState: ReadAloudState = ReadAloudState.Idle,
    /** 朗读失败提示，展示后调用 dismissSpeechError() 清掉 */
    val speechError: String? = null,
) {
    val isGenerating: Boolean
        get() = generation == GenerationState.Connecting ||
            generation == GenerationState.Reasoning ||
            generation == GenerationState.Streaming

    val isEmpty: Boolean get() = messages.isEmpty()
}
