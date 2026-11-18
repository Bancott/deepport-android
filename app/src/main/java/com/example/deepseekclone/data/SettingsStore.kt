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

    /** 个性化：思考结束后是否自动折叠思考内容（官方默认开启） */
    var autoCollapseReasoning by mutableStateOf(prefs.getBoolean(KEY_AUTO_COLLAPSE, true))
        private set

    var model by mutableStateOf(
        prefs.getString(KEY_MODEL, "").orEmpty(),
    )
        private set

    /** 朗读音色：mira（贝壳）/ echo（白浪）/ stella（海星）/ tide（暗潮） */
    var ttsVoice by mutableStateOf(
        prefs.getString(KEY_TTS_VOICE, DEFAULT_TTS_VOICE).orEmpty().ifBlank { DEFAULT_TTS_VOICE },
    )
        private set

    fun updateApiKey(value: String) {
        apiKey = value
        persist()
    }

    fun updateTtsVoice(value: String) {
        // 按 TTS_VOICES 校验，别硬编码：之前只认 tide / stella，选贝壳或白浪会被静默改回海星
        val clean = TTS_VOICES.firstOrNull { it.first == value }?.first ?: DEFAULT_TTS_VOICE
        ttsVoice = clean
        prefs.edit().putString(KEY_TTS_VOICE, clean).apply()
    }

    fun updateBaseUrl(value: String) {
        baseUrl = value
        persist()
    }

    /** 充值页登录后抓到的昵称（脱敏手机号）与头像地址；空串表示未登录 */
    var userName by mutableStateOf(prefs.getString("user_name", "").orEmpty())
        private set
    var userAvatar by mutableStateOf(prefs.getString("user_avatar", "").orEmpty())
        private set

    fun updateUserInfo(name: String, avatar: String) {
        if (name == userName && avatar == userAvatar) return
        userName = name
        userAvatar = avatar
        prefs.edit().putString("user_name", name).putString("user_avatar", avatar).apply()
    }

    fun updateThemeMode(value: String) {
        themeMode = value
        prefs.edit().putString(KEY_THEME, value).apply()
    }

    fun updateFontScale(value: Float) {
        fontScale = value
        prefs.edit().putFloat(KEY_FONT, value).apply()
    }

    fun updateAutoCollapseReasoning(value: Boolean) {
        autoCollapseReasoning = value
        prefs.edit().putBoolean(KEY_AUTO_COLLAPSE, value).apply()
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
            "mira" to "贝壳",
            "echo" to "白浪",
            "stella" to "海星",
            "tide" to "暗潮",
        )

        /**
         * `/v1/tools/tts` 实际只认这两个 voice_id，其余会返回
         * `invalid_request_error: supported values are 'stella' (default), 'tide'`。
         * 贝壳 / 白浪 是官方 App 的内测音色，接口层尚未开放。
         */
        val TTS_SUPPORTED_VOICES = setOf("stella", "tide")
        val MODELS = listOf(
            "deepseek-chat" to "通用对话（V3）",
            "deepseek-reasoner" to "深度思考（R1）",
        )

        private const val KEY_API = "api_key"
        private const val KEY_BASE = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_FONT = "font_scale"
        private const val KEY_AUTO_COLLAPSE = "auto_collapse_reasoning"
        private const val KEY_TTS_VOICE = "tts_voice"
    }
}



