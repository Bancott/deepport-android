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
import com.example.deepseekclone.ui.chat.ChatScreen
import com.example.deepseekclone.ui.chat.ChatUiState
import com.example.deepseekclone.ui.chat.GenerationState
import com.example.deepseekclone.ui.chat.InputMode
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
 * 第一阶段仅为可视化验收服务：状态用 remember 托管，
 * 第 5 阶段接入 ChatViewModel(StateFlow) 后删除。
 */
@Composable
private fun ChatDemo() {
    var state by remember { mutableStateOf(ChatUiState()) }

    ChatScreen(
        state = state,
        onMenuClick = { },
        onNewChatClick = { state = ChatUiState() },
        onToggleSpeechOutput = {
            state = state.copy(isSpeechOutputOn = !state.isSpeechOutputOn)
        },
        onTextChange = { text ->
            state = state.copy(input = state.input.copy(text = text))
        },
        onToggleDeepThinking = {
            state = state.copy(input = state.input.copy(deepThinking = !state.input.deepThinking))
        },
        onToggleSmartSearch = {
            state = state.copy(input = state.input.copy(smartSearch = !state.input.smartSearch))
        },
        onAttachClick = { },
        onToggleInputMode = {
            val next = if (state.input.mode == InputMode.Text) InputMode.Voice else InputMode.Text
            state = state.copy(input = state.input.copy(mode = next))
        },
        onDismissTranscribedNotice = {
            state = state.copy(input = state.input.copy(transcribedNotice = null))
        },
        onSend = {
            if (state.input.canSend) {
                state = state.copy(
                    generation = GenerationState.Streaming,
                    input = state.input.copy(text = "", transcribedNotice = null),
                )
            }
        },
        onStop = {
            state = state.copy(generation = GenerationState.Idle)
        },
    )
}
