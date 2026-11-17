package com.example.deepseekclone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.deepseekclone.ui.chat.ChatRole
import com.example.deepseekclone.ui.chat.ChatScreen
import com.example.deepseekclone.ui.chat.ChatUiState
import com.example.deepseekclone.ui.chat.GenerationState
import com.example.deepseekclone.ui.chat.InputMode
import com.example.deepseekclone.ui.chat.MessageStatus
import com.example.deepseekclone.ui.chat.MessageUi
import com.example.deepseekclone.ui.chat.TokenUsage
import com.example.deepseekclone.ui.theme.DeepSeekTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DeepSeekTheme {
                ChatDemo()
            }
        }
    }
}

/**
 * 第一阶段可视化宿主：用 remember 托管状态，覆盖 7 种消息状态。
 * 接入 ChatViewModel(StateFlow) 后整体替换。
 */
@Composable
private fun ChatDemo() {
    var state by remember { mutableStateOf(ChatUiState(title = "复刻 DeepSeek 安卓架构", messages = demoConversation(), sessionTokens = 1732)) }
    var counter by remember { mutableStateOf(0) }

    ChatScreen(
        state = state,
        onNewChatClick = {
            state = ChatUiState(greeting = "嗨！今天想聊些什么？")
        },
        onToggleSpeechOutput = {
            state = state.copy(isSpeechOutputOn = !state.isSpeechOutputOn)
        },
        onTextChange = { text -> state = state.copy(input = state.input.copy(text = text)) },
        onToggleDeepThinking = {
            state = state.copy(input = state.input.copy(deepThinking = !state.input.deepThinking))
        },
        onToggleSmartSearch = {
            state = state.copy(input = state.input.copy(smartSearch = !state.input.smartSearch))
        },
        onToggleInputMode = {
            val next = if (state.input.mode == InputMode.Text) InputMode.Voice else InputMode.Text
            state = state.copy(input = state.input.copy(mode = next))
        },
        onDismissTranscribedNotice = {
            state = state.copy(input = state.input.copy(transcribedNotice = null))
        },
        onSend = {
            if (state.input.canSend) {
                counter += 1
                val text = state.input.text
                state = state.copy(
                    title = if (state.title.isBlank()) "新对话" else state.title,
                    generation = GenerationState.Connecting,
                    input = state.input.copy(text = "", transcribedNotice = null),
                    messages = state.messages + listOf(
                        MessageUi(
                            id = "u-$counter",
                            role = ChatRole.User,
                            text = text,
                            status = MessageStatus.Sent,
                        ),
                        MessageUi(
                            id = "a-$counter",
                            role = ChatRole.Assistant,
                            status = MessageStatus.Connecting,
                        ),
                    ),
                )
            }
        },
        onStop = {
            state = state.copy(
                generation = GenerationState.Idle,
                messages = state.messages.map {
                    if (it.status == MessageStatus.Connecting || it.status == MessageStatus.Streaming) {
                        it.copy(status = MessageStatus.Aborted)
                    } else {
                        it
                    }
                },
            )
        },
        onToggleReasoning = { message ->
            state = state.copy(
                messages = state.messages.map {
                    if (it.id == message.id) it.copy(reasoningExpanded = !it.reasoningExpanded) else it
                },
            )
        },
        onRegenerate = {
            state = state.copy(messages = state.messages)
        },
    )
}

/* ------------------------------- Demo 数据 ------------------------------- */

private const val REASONING = "用户想要一个和 DeepSeek 安卓端一模一样的客户端。关键点：不能只是 UI 还原，要处理流式、树状会话、分支、附件、语音。先给分层架构，再讲流式状态机和分支模型。避免堆代码，先给决策和取舍。"

private const val ANSWER = """复刻一个与 DeepSeek 安卓 App“一模一样”的客户端，架构设计的核心矛盾不在于 UI 还原度，而在于流式体验的稳定性与会话状态的一致性。DeepSeek 官方开源的 `chat-demo` 已经揭示了产品内核：会话是一棵树，而不是一条扁平的消息数组。

---

## 顶层决策：原生重写，还是 Web-First？

在深入细节前，你需要先选择一条基础路线，二者在“复刻程度”与“工程代价”上差异巨大。

## 1. 先明确：做什么、给谁用、解决什么问题

写一句话描述：

- 用户是谁？
- 他遇到什么问题？
- 你的软件怎么解决？
- 最核心的 1 个功能是什么？

### 常见坑

- 一开始功能太多，做不完
- 只学不练，永远停在教程
- 不备份代码，不用 Git"""

private const val STREAMING = "做一个软件可以按“从想法到上线”的流程推进。核心不是先学完所有编程，而是先做一个最小可用版本（MVP），再迭代。"

private fun demoConversation(): List<MessageUi> = listOf(
    MessageUi(
        id = "m1",
        role = ChatRole.User,
        text = "由于 deepseek 公布了全家桶 api，我想自己复刻出与 deepseek 安卓版 app 一模一样的 APP。你帮我提出一下架构",
        status = MessageStatus.Sent,
    ),
    MessageUi(
        id = "m2",
        role = ChatRole.Assistant,
        text = ANSWER,
        reasoning = REASONING,
        status = MessageStatus.Completed,
        usage = TokenUsage(prompt = 812, completion = 472, reasoning = 318),
        reasoningSeconds = 10,
        sourcesCount = 22,
        branchIndex = 2,
        branchCount = 2,
    ),
    MessageUi(
        id = "m3",
        role = ChatRole.User,
        text = "如何做一个软件",
        status = MessageStatus.Sent,
    ),
    MessageUi(
        id = "m4",
        role = ChatRole.Assistant,
        text = STREAMING,
        reasoning = "用户想从零做一个软件。给最小可行路径：定目标、做 MVP、选技术栈、上线、迭代。不要一上来讲全家桶。",
        status = MessageStatus.Streaming,
        reasoningSeconds = 2,
        reasoningExpanded = true,
        usage = TokenUsage(prompt = 320, completion = 128),
    ),
    MessageUi(
        id = "m5",
        role = ChatRole.User,
        text = "网络断了会怎样？",
        status = MessageStatus.Sent,
    ),
    MessageUi(
        id = "m6",
        role = ChatRole.Assistant,
        text = "网络中断时会保留已经生成的内容，并在下方给出重试入口，不会把整段回答丢掉。",
        status = MessageStatus.Failed,
        errorHint = "请检查网络后重试",
    ),
)

