package io.github.bancott.deepport.data

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread
import kotlin.math.sqrt

/**
 * 按住说话的录音器：官方 ASR 接口要 WAV（单声道 16bit PCM），
 * 所以用 AudioRecord 直接录裸 PCM，边录边写 WAV 头占位，结束后回填长度。
 */
class VoiceRecorder(context: Context) {

    private val app = context.applicationContext
    private var record: AudioRecord? = null
    private var worker: Thread? = null
    private var file: File? = null

    @Volatile
    private var running = false

    @Volatile
    private var currentLevel = 0f

    @Volatile
    private var bytesWritten = 0

    val isRecording: Boolean get() = running

    fun start(): Boolean {
        if (running) return false
        val sampleRate = SAMPLE_RATE
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return false
        val bufferSize = minBuffer * 2
        val recorder = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                )
            } else {
                @Suppress("DEPRECATION")
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                )
            }
        }.getOrNull() ?: return false

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return false
        }

        val target = File(app.cacheDir, "voice_${System.currentTimeMillis()}.wav")
        val ok = runCatching {
            target.outputStream().use { it.write(wavHeader(0)) }
            recorder.startRecording()
        }.isSuccess
        if (!ok) {
            recorder.release()
            return false
        }

        record = recorder
        file = target
        running = true
        bytesWritten = 0
        currentLevel = 0f

        worker = thread(name = "voice-recorder") {
            val buffer = ByteArray(bufferSize)
            target.outputStream().use { out ->
                out.write(wavHeader(0))
                while (running) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    out.write(buffer, 0, read)
                    bytesWritten += read
                    currentLevel = rms(buffer, read)
                }
            }
        }
        return true
    }

    /** 结束录音并回填 WAV 头；返回可上传的文件 */
    fun stop(): File? {
        if (!running) return null
        running = false
        runCatching { worker?.join(1500) }
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
        worker = null
        val target = file ?: return null
        file = null
        patchWavHeader(target, bytesWritten)
        return target.takeIf { it.length() > 44 }
    }

    fun cancel() {
        running = false
        runCatching { worker?.join(800) }
        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null
        worker = null
        file?.delete()
        file = null
    }

    /** 当前电平 0f..1f，给波形用 */
    fun level(): Float = currentLevel

    private fun rms(buffer: ByteArray, length: Int): Float {
        if (length < 2) return 0f
        var sum = 0.0
        var i = 0
        while (i + 1 < length) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
            sum += (sample.toDouble() / 32768.0) * (sample.toDouble() / 32768.0)
            i += 2
        }
        val value = sqrt(sum / (length / 2))
        return (value * 6.0).coerceIn(0.0, 1.0).toFloat()
    }

    private fun wavHeader(dataLength: Int): ByteArray {
        val total = dataLength + 36
        val header = ByteArray(44)
        fun putString(offset: Int, value: String) {
            value.toByteArray(Charsets.US_ASCII).copyInto(header, offset)
        }

        fun putInt(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
            header[offset + 2] = ((value shr 16) and 0xFF).toByte()
            header[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }

        fun putShort(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        putString(0, "RIFF")
        putInt(4, total)
        putString(8, "WAVE")
        putString(12, "fmt ")
        putInt(16, 16)
        putShort(20, 1) // PCM
        putShort(22, 1) // 单声道
        putInt(24, SAMPLE_RATE)
        putInt(28, SAMPLE_RATE * 2)
        putShort(32, 2)
        putShort(34, 16)
        putString(36, "data")
        putInt(40, dataLength)
        return header
    }

    private fun patchWavHeader(target: File, dataLength: Int) {
        runCatching {
            RandomAccessFile(target, "rw").use { raf ->
                raf.seek(0)
                raf.write(wavHeader(dataLength))
            }
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
    }
}
