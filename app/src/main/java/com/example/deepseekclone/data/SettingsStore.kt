package com.example.deepseekclone.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 本地配置存储（Data 层最小实现）。
 * 后续换成 DataStore / Room 时，只替换这里，UI 不用动。
 */
class SettingsStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ds_settings", Context.MODE_PRIVATE)

    var apiKey by mutableStateOf(prefs.getString(KEY_API, "").orEmpty())
        private set

    var baseUrl by mutableStateOf(
        prefs.getString(KEY_BASE, DEFAULT_BASE).orEmpty().ifBlank { DEFAULT_BASE },
    )
        private set

    /** system / light / dark */
    var themeMode by mutableStateOf(prefs.getString(KEY_THEME, "system").orEmpty())
        private set

    /** 字体缩放：0 = 跟随系统 */
    var fontScale by mutableStateOf(prefs.getFloat(KEY_FONT, 0f))
        private set

    var model by mutableStateOf(
        prefs.getString(KEY_MODEL, "").orEmpty(),
    )
        private set

    /** 朗读音色：stella（海星，女声）/ tide（暗潮，男声） */
    var ttsVoice by mutableStateOf(
        prefs.getString(KEY_TTS_VOICE, DEFAULT_TTS_VOICE).orEmpty().ifBlank { DEFAULT_TTS_VOICE },
    )
        private set

    fun updateApiKey(value: String) {
        apiKey = value
        persist()
    }

    fun updateTtsVoice(value: String) {
        val clean = if (value == "tide") "tide" else "stella"
        ttsVoice = clean
        prefs.edit().putString(KEY_TTS_VOICE, clean).apply()
    }

    fun updateBaseUrl(value: String) {
        baseUrl = value
        persist()
    }

    fun updateThemeMode(value: String) {
        themeMode = value
        prefs.edit().putString(KEY_THEME, value).apply()
    }

    fun updateFontScale(value: Float) {
        fontScale = value
        prefs.edit().putFloat(KEY_FONT, value).apply()
    }

    fun updateModel(value: String) {
        model = value
        persist()
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY_API, apiKey)
            .putString(KEY_BASE, baseUrl)
            .putString(KEY_MODEL, model)
            .apply()
    }

    companion object {
        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore = instance ?: synchronized(this) {
            instance ?: SettingsStore(context).also { instance = it }
        }

        const val DEFAULT_BASE = "https://api.deepseek.com"
        /** 模型 id 由 GET /models 决定，本地不写死 */
        const val DEFAULT_MODEL = ""
        const val DEFAULT_TTS_VOICE = "stella"
        /** 官方接口目前只开放这两档音色 */
        val TTS_VOICES = listOf(
            "stella" to "海星",
            "tide" to "暗潮",
        )
        val MODELS = listOf(
            "deepseek-chat" to "通用对话（V3）",
            "deepseek-reasoner" to "深度思考（R1）",
        )

        private const val KEY_API = "api_key"
        private const val KEY_BASE = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_FONT = "font_scale"
        private const val KEY_TTS_VOICE = "tts_voice"
    }
}



