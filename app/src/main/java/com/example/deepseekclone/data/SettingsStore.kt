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

    /* ------------------------- 语音识别（可选，按住说话） ------------------------- */

    /** OpenAI 兼容的 /audio/transcriptions 地址，如 https://api.openai.com/v1 */
    var asrBaseUrl by mutableStateOf(prefs.getString(KEY_ASR_BASE, "").orEmpty())
        private set

    /** 留空则复用上面的 API Key */
    var asrApiKey by mutableStateOf(prefs.getString(KEY_ASR_KEY, "").orEmpty())
        private set

    var asrModel by mutableStateOf(
        prefs.getString(KEY_ASR_MODEL, DEFAULT_ASR_MODEL).orEmpty(),
    )
        private set

    fun updateApiKey(value: String) {
        apiKey = value
        persist()
    }

    fun updateAsrBaseUrl(value: String) {
        asrBaseUrl = value.trim()
        prefs.edit().putString(KEY_ASR_BASE, value.trim()).apply()
    }

    fun updateAsrApiKey(value: String) {
        asrApiKey = value.trim()
        prefs.edit().putString(KEY_ASR_KEY, value.trim()).apply()
    }

    fun updateAsrModel(value: String) {
        asrModel = value.trim()
        prefs.edit().putString(KEY_ASR_MODEL, value.trim()).apply()
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
        /** 语音识别默认模型名（多数 OpenAI 兼容网关都用这个） */
        const val DEFAULT_ASR_MODEL = "whisper-1"
        val MODELS = listOf(
            "deepseek-chat" to "通用对话（V3）",
            "deepseek-reasoner" to "深度思考（R1）",
        )

        private const val KEY_API = "api_key"
        private const val KEY_BASE = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_FONT = "font_scale"
        private const val KEY_ASR_BASE = "asr_base_url"
        private const val KEY_ASR_KEY = "asr_api_key"
        private const val KEY_ASR_MODEL = "asr_model"
    }
}



