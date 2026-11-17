package com.example.deepseekclone.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * 系统 TTS 朗读（动作条喇叭 / 顶栏自动朗读都走这里）。
 * 引擎异步初始化，初始化完成前调用 speak 会先记下来，就绪后补读。
 */
class Speaker(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    /** 朗读开始 / 结束的回调，用来在 UI 上标记"正在朗读" */
    var onStateChange: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (!ready) {
                android.util.Log.w("Speaker", "TTS 引擎初始化失败 status=$status")
                return@TextToSpeech
            }
            val engine = tts ?: return@TextToSpeech
            val result = engine.setLanguage(Locale.CHINA)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.getDefault())
            }
            engine.setSpeechRate(1.02f)
            engine.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onStateChange?.invoke(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onStateChange?.invoke(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onStateChange?.invoke(false)
                    }
                },
            )
            pending?.let { text ->
                pending = null
                speak(text)
            }
        }
    }

    fun speak(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val engine = tts
        if (engine == null || !ready) {
            pending = clean
            android.util.Log.i("Speaker", "引擎未就绪，先挂起 ${clean.length} 字")
            return
        }
        engine.stop()
        val result = engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "ds-${System.currentTimeMillis()}")
        android.util.Log.i("Speaker", "speak ${clean.length} 字 → $result")
    }

    fun stop() {
        pending = null
        runCatching { tts?.stop() }
        onStateChange?.invoke(false)
    }

    fun shutdown() {
        pending = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }

    companion object {
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
