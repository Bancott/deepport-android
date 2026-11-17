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

/** 待发送附件 */
data class AttachmentUi(
    val id: String,
    val displayName: String,
    val isImage: Boolean = false,
    /** null 表示未开始上传；0f..1f 为进度 */
    val progress: Float? = null,
)

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

/** 聊天页整体 UiState（后续接入 ViewModel 时只暴露这一个对象） */
data class ChatUiState(
    /** 空会话时官方不显示标题，第一条消息发出后才是"新对话" */
    val title: String = "",
    val greeting: String = "嗨！今天想聊些什么？",
    val generation: GenerationState = GenerationState.Idle,
    val input: InputUiState = InputUiState(),
    val isSpeechOutputOn: Boolean = false,
) {
    val isGenerating: Boolean
        get() = generation == GenerationState.Connecting ||
            generation == GenerationState.Reasoning ||
            generation == GenerationState.Streaming
}
