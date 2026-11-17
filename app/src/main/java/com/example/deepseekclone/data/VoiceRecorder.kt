package com.example.deepseekclone.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * 按住说话的录音器：AAC / 16k 单声道，输出到 cacheDir。
 * stop() 返回文件，cancel() 直接删掉。
 */
class VoiceRecorder(context: Context) {

    private val app = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    val isRecording: Boolean get() = recorder != null

    fun start(): Boolean {
        if (recorder != null) return false
        val target = File(app.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val created = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(app)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        return runCatching {
            created.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(target.absolutePath)
                prepare()
                start()
            }
            recorder = created
            file = target
            true
        }.getOrElse {
            runCatching { created.release() }
            recorder = null
            file = null
            false
        }
    }

    /** 结束并返回录音文件；失败返回 null */
    fun stop(): File? {
        val current = recorder ?: return null
        recorder = null
        val result = file
        file = null
        runCatching { current.stop() }
        runCatching { current.release() }
        return result?.takeIf { it.exists() && it.length() > 0 }
    }

    fun cancel() {
        val current = recorder
        recorder = null
        val target = file
        file = null
        runCatching { current?.stop() }
        runCatching { current?.release() }
        runCatching { target?.delete() }
    }

    /** 当前输入电平 0f..1f，用于面板上的波形 */
    fun level(): Float {
        val amplitude = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return (amplitude / 32_767f).coerceIn(0f, 1f)
    }
}
