package io.github.bancott.deepport.data

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
    /** 供应商：deepseek / kimi / qwen / custom */
    var provider by mutableStateOf(prefs.getString(KEY_PROVIDER, "deepseek").orEmpty())
    /** 协议：anthropic（DeepSeek 原生） / openai（Kimi、千问等兼容接口） */
    var protocol by mutableStateOf(prefs.getString(KEY_PROTOCOL, "anthropic").orEmpty())
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

    /** 个性化：角色定义（附加到 system 提示的自定义身份/语气/约束） */
    var roleDefinition by mutableStateOf(prefs.getString(KEY_ROLE, "").orEmpty())
        private set

    /** 个性化：采样温度 0.0–2.0，默认 1.0 */
    var temperature by mutableStateOf(prefs.getFloat(KEY_TEMPERATURE, 1.0f))
        private set

    /** 个性化：思考强度 0=低 1=中 2=高（深度思考开启时的推理预算） */
    var thinkingEffort by mutableStateOf(prefs.getInt(KEY_THINKING_EFFORT, 1))
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

    fun updateProvider(value: String, newBaseUrl: String, newProtocol: String) {
        provider = value
        baseUrl = newBaseUrl
        protocol = newProtocol
        prefs.edit()
            .putString(KEY_PROVIDER, value)
            .putString(KEY_BASE, newBaseUrl)
            .putString(KEY_PROTOCOL, newProtocol)
            .apply()
    }

    fun updateProtocol(value: String) {
        protocol = value
        prefs.edit().putString(KEY_PROTOCOL, value).apply()
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

    fun updateRoleDefinition(value: String) {
        roleDefinition = value
        prefs.edit().putString(KEY_ROLE, value).apply()
    }

    fun updateTemperature(value: Float) {
        temperature = value.coerceIn(0f, 2f)
        prefs.edit().putFloat(KEY_TEMPERATURE, temperature).apply()
    }

    fun updateThinkingEffort(value: Int) {
        thinkingEffort = value.coerceIn(0, 2)
        prefs.edit().putInt(KEY_THINKING_EFFORT, thinkingEffort).apply()
    }

    /** 思考强度 → thinking.budget_tokens（官方 Anthropic 兼容接口用预算控制推理长度） */
    fun thinkingBudgetTokens(): Int = when (thinkingEffort) {
        0 -> 1024
        2 -> 8192
        else -> 4096
    }

    fun updateModel(value: String) {
        model = value
        persist()
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY_API, apiKey)
            .putString(KEY_BASE, baseUrl)
        .putString(KEY_PROVIDER, provider)
        .putString(KEY_PROTOCOL, protocol)
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

        private const val KEY_PROVIDER = "provider"
private const val KEY_PROTOCOL = "protocol"
private const val KEY_API = "api_key"
        private const val KEY_BASE = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_FONT = "font_scale"
        private const val KEY_AUTO_COLLAPSE = "auto_collapse_reasoning"
        private const val KEY_ROLE = "role_definition"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_THINKING_EFFORT = "thinking_effort"
        private const val KEY_TTS_VOICE = "tts_voice"
    }
}



