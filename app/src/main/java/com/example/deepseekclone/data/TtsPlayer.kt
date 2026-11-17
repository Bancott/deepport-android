package com.example.deepseekclone.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 朗读播放器：把 `/v1/tools/tts` 的裸 PCM（24kHz / 单声道 / 16bit 小端）边收边写进 AudioTrack。
 *
 * 首块数据到达即出声；同一时间只读一条，[stop] 会立刻静音（pause + flush），
 * 正常读完则等缓冲区播完再释放，避免尾音被截断。
 * 状态经 [state] 暴露给 UI，失败原因经 [error] 冒出去做 toast。
 */
class TtsPlayer {

    enum class State { Idle, Loading, Playing }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var job: Job? = null

    @Volatile
    private var activeTrack: AudioTrack? = null

    /** 每次播放 / 停止都自增，用来丢弃旧任务迟到的状态与错误 */
    private var token = 0

    fun play(stream: Flow<ByteArray>) {
        val my = ++token
        job?.cancel()
        _error.value = null
        _state.value = State.Loading
        job = scope.launch {
            try {
                withContext(Dispatchers.IO) { consume(stream, my) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("TtsPlayer", "play failed", e)
                if (my == token) {
                    _error.value = e.message?.takeIf { it.isNotBlank() } ?: "朗读失败，请稍后重试"
                }
            } finally {
                if (my == token) _state.value = State.Idle
            }
        }
    }

    fun stop() {
        token++
        job?.cancel()
        job = null
        // 先静音，再让 IO 任务在 write 返回后自行 release
        activeTrack?.let { track ->
            runCatching { track.pause() }
            runCatching { track.flush() }
        }
        _state.value = State.Idle
    }

    fun clearError() {
        _error.value = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun consume(stream: Flow<ByteArray>, my: Int) {
        var audio: AudioTrack? = null
        var writtenBytes = 0L
        try {
            stream.collect { chunk ->
                if (my != token) return@collect
                if (writtenBytes == 0L) {
                    // 服务端合成失败时可能回一段 JSON 错误而不是 PCM，别把 JSON 当噪音播出去
                    jsonError(chunk)?.let { throw IllegalStateException(it) }
                }
                val track = audio ?: newTrack().also {
                    audio = it
                    activeTrack = it
                    it.play()
                    _state.value = State.Playing
                }
                var offset = 0
                var idleWrites = 0
                while (offset < chunk.size && my == token) {
                    val written = track.write(chunk, offset, chunk.size - offset)
                    if (written < 0) break
                    if (written == 0) {
                        // 缓冲区满时 blocking write 偶发返回 0，短暂等待后重试，避免丢帧
                        if (++idleWrites > 50) break
                        delay(10)
                        continue
                    }
                    idleWrites = 0
                    offset += written
                    writtenBytes += written
                }
            }
            if (my != token) return
            if (audio == null) throw IllegalStateException("没有收到音频数据，请稍后重试")
            Log.i("TtsPlayer", "stream done: ${writtenBytes / 2 / SAMPLE_RATE.toLong()}s audio")
            // 等最后一帧播完（16bit 单声道，2 字节一帧）
            awaitDrain(audio, writtenBytes / 2, my)
        } finally {
            audio?.let { track ->
                activeTrack = null
                if (my == token) {
                    runCatching { track.stop() }
                } else {
                    runCatching { track.pause() }
                    runCatching { track.flush() }
                }
                runCatching { track.release() }
            }
        }
    }

    private suspend fun awaitDrain(track: AudioTrack, frames: Long, my: Int) {
        var waited = 0
        while (my == token &&
            track.playbackHeadPosition.toLong() < frames &&
            waited < DRAIN_TIMEOUT_MS
        ) {
            delay(40)
            waited += 40
        }
        Log.i("TtsPlayer", "drain done head=${track.playbackHeadPosition}/$frames")
    }

    private fun newTrack(): AudioTrack {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val bufferBytes = if (minBuffer > 0) minBuffer * 4 else 64 * 1024
        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferBytes)
            .build()
    }

    private fun jsonError(chunk: ByteArray): String? {
        if (chunk.isEmpty() || chunk[0] != '{'.code.toByte()) return null
        val text = String(chunk, Charsets.UTF_8)
        return runCatching {
            JSONObject(text).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "语音合成失败，请稍后重试"
    }

    companion object {
        /** TTS 输出的裸 PCM 参数：24kHz、单声道、16-bit 小端 */
        const val SAMPLE_RATE = 24_000
        private const val DRAIN_TIMEOUT_MS = 8_000

        /** Markdown → 适合朗读的纯文本：去掉代码块、强调符号和链接语法 */
        fun plainText(markdown: String): String = markdown
            .replace(Regex("```[\\s\\S]*?```"), " 代码块 ")
            .replace(Regex("`([^`]*)`"), "$1")
            .replace(Regex("!?\\[([^\\]]*)]\\([^)]*\\)"), "$1")
            .replace(Regex("(?m)^#{1,6}\\s*"), "")
            .replace(Regex("[*_>#~|]"), "")
            .replace(Regex("\\n{2,}"), "\n")
            .trim()
    }
}
