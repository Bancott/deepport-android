package io.github.bancott.deepport.ui.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.github.bancott.deepport.data.VoiceRecorder
import kotlinx.coroutines.delay
import java.io.File

/**
 * 「按住说话」的录音状态机：按下开录、上滑进入取消态、松手出文件。
 * 录音文件交给 onRecorded（上层去做转写），权限被拒交给 onDenied。
 */
@Stable
class VoiceInputState internal constructor(
    private val recorder: VoiceRecorder,
    private val requestPermission: () -> Unit,
    private val onRecorded: (File) -> Unit,
) {
    var recording by mutableStateOf(false)
        private set
    var cancelHint by mutableStateOf(false)
        private set
    /** 最近 24 帧电平，面板上的波形直接用 */
    val levels = mutableStateListOf<Float>()

    internal fun start() {
        if (recording) return
        if (recorder.start()) {
            recording = true
            cancelHint = false
            levels.clear()
        } else {
            requestPermission()
        }
    }

    fun updateLevel(level: Float) {
        levels.add(level)
        while (levels.size > 24) levels.removeAt(0)
    }

    fun updateCancel(hint: Boolean) {
        cancelHint = hint
    }

    /**
     * 松手：cancel=true 直接丢弃录音，否则把文件交给上层转写。
     * 录音过短（< 0.4s）也当误触丢掉，避免空请求。
     */
    fun finish(cancel: Boolean) {
        if (!recording) return
        recording = false
        cancelHint = false
        levels.clear()
        if (cancel) {
            recorder.cancel()
            return
        }
        val file = recorder.stop()
        if (file == null || file.length() < 1024) {
            file?.delete()
            return
        }
        onRecorded(file)
    }

    /** 录音被系统打断 / 页面销毁时兜底 */
    fun abort() {
        if (!recording) return
        recording = false
        cancelHint = false
        levels.clear()
        recorder.cancel()
    }
}

@Composable
fun rememberVoiceInput(
    onRecorded: (File) -> Unit,
    onDenied: () -> Unit,
): VoiceInputState {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok -> if (!ok) onDenied() }
    val recorder = remember { VoiceRecorder(context) }
    val state = remember(recorder) {
        VoiceInputState(
            recorder = recorder,
            // 没有麦克风权限时开录会失败，这里顺势走一次系统授权
            requestPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
            onRecorded = onRecorded,
        )
    }

    LaunchedEffect(state.recording) {
        while (state.recording) {
            state.updateLevel(recorder.level())
            delay(60)
        }
    }

    DisposableEffect(Unit) {
        onDispose { state.abort() }
    }
    return state
}

private fun Context.hasRecordAudio(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
