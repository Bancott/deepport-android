package com.example.deepseekclone.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.data.SettingsStore
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * 模型设置（复刻版新增页）：API Key / Base URL / 模型 / 连通性测试。
 * 这是唯一需要用户自己填 Key 的地方，官方 App 没有这一页。
 */
@Composable
fun ModelSettingsScreen(
    store: SettingsStore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    var keyVisible by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var resultOk by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgGrouped)
            .statusBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            CircleIconButton(
                contentDescription = "返回",
                onClick = onBack,
                containerColor = colors.surface2,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "模型设置",
                style = DsText.h2.copy(fontSize = 18.sp),
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp),
        ) {
            SectionLabel("接口")
            SettingsCard {
                FieldRow(
                    title = "API Key",
                    value = store.apiKey,
                    placeholder = "sk-...",
                    masked = !keyVisible,
                    onValueChange = store::updateApiKey,
                    trailing = {
                        CircleIconButton(
                            contentDescription = if (keyVisible) "隐藏 Key" else "显示 Key",
                            onClick = { keyVisible = !keyVisible },
                            size = 32.dp,
                        ) {
                            Icon(
                                imageVector = if (keyVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    },
                )
                Divider()
                FieldRow(
                    title = "Base URL",
                    value = store.baseUrl,
                    placeholder = SettingsStore.DEFAULT_BASE,
                    masked = false,
                    onValueChange = store::updateBaseUrl,
                )
            }

            SectionLabel("模型")
            SettingsCard {
                SettingsStore.MODELS.forEachIndexed { index, (id, desc) ->
                    ModelRow(
                        id = id,
                        desc = desc,
                        selected = store.model == id,
                        onClick = { store.updateModel(id) },
                    )
                    if (index != SettingsStore.MODELS.lastIndex) Divider()
                }
            }

            Spacer(Modifier.height(20.dp))

            // 连通性测试
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (testing) colors.surface2 else colors.primary)
                    .clickable(enabled = !testing) {
                        testing = true
                        result = null
                        scope.launch {
                            val r = testConnection(store.baseUrl, store.apiKey)
                            resultOk = r.startsWith("连接成功")
                            result = r
                            testing = false
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (testing) "正在测试…" else "测试连接",
                    style = DsText.button,
                    color = if (testing) colors.textSecondary else colors.onPrimary,
                )
            }

            result?.let { text ->
                Text(
                    text = text,
                    style = DsText.caption,
                    color = if (resultOk) colors.textSecondary else colors.danger,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }

            Text(
                text = "Key 只保存在本机，用于调用 DeepSeek 开放平台接口；本 App 不代理、不上传你的 Key。",
                style = DsText.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = DsText.caption,
        color = DsTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = DsTheme.colors.surface2,
    ) {
        Column { content() }
    }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(DsTheme.colors.outline),
    )
}

@Composable
private fun FieldRow(
    title: String,
    value: String,
    placeholder: String,
    masked: Boolean,
    onValueChange: (String) -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = DsText.label.copy(fontSize = 15.sp),
            color = colors.textSecondary,
            modifier = Modifier.width(76.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = DsText.label.copy(color = colors.textPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(colors.primary),
            visualTransformation = if (masked) PasswordVisualTransformation() else VisualTransformation.None,
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = DsText.label.copy(fontSize = 15.sp),
                        color = colors.textTertiary,
                    )
                }
                inner()
            },
        )
        trailing?.invoke()
    }
}

@Composable
private fun ModelRow(
    id: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 6.dp else 1.5.dp,
                    color = if (selected) colors.textPrimary else colors.textTertiary,
                    shape = CircleShape,
                ),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = id,
                style = DsText.label,
                color = colors.textPrimary,
            )
            Text(
                text = desc,
                style = DsText.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary,
            )
        }
    }
}

/** 极简连通性测试：GET {base}/models，返回状态码即可（不引入 OkHttp）。 */
private suspend fun testConnection(baseUrl: String, apiKey: String): String =
    withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext "尚未填写 API Key"
        val url = baseUrl.trimEnd('/') + "/models"
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            conn.disconnect()
            when {
                code in 200..299 -> "连接成功（HTTP $code），可以开始对话"
                code == 401 -> "连接失败：API Key 无效（HTTP 401）"
                code == 403 -> "连接失败：无权限（HTTP 403）"
                else -> "连接失败：HTTP $code"
            }
        } catch (e: Exception) {
            "连接失败：${e.javaClass.simpleName} ${e.message.orEmpty()}"
        }
    }
