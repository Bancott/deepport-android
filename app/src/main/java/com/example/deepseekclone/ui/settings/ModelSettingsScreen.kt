package com.example.deepseekclone.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.data.ModelInfo
import com.example.deepseekclone.data.ModelRepository
import com.example.deepseekclone.data.SettingsStore
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme
import kotlinx.coroutines.launch

/**
 * 模型设置（复刻版新增页）。
 * 模型列表不写死：填写 Key 后自动请求 `GET {BaseURL}/models`，
 * 用供应商返回的 id / 名称 / 上下文长度渲染，选中的 id 存本地。
 */
@Composable
fun ModelSettingsScreen(
    store: SettingsStore,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    repository: ModelRepository = remember { ModelRepository() },
) {
    val colors = DsTheme.colors
    var keyVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf<List<ModelInfo>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadModels() {
        if (loading) return
        if (store.apiKey.isBlank()) {
            statusOk = false
            status = "请先填写 API Key"
            return
        }
        loading = true
        status = null
        scope.launch {
            val result = repository.fetchModels(store.baseUrl, store.apiKey)
            result.onSuccess { list ->
                models = list
                statusOk = list.isNotEmpty()
                status = if (list.isEmpty()) {
                    "接口未返回任何模型"
                } else {
                    "已获取 ${list.size} 个模型，选择后即可对话"
                }
                if (list.isNotEmpty() && list.none { it.id == store.model }) {
                    store.updateModel(list.first().id)
                }
            }.onFailure {
                statusOk = false
                status = it.message ?: "获取模型失败"
            }
            loading = false
        }
    }

    // 填了 Key 就自动拉一次列表
    LaunchedEffect(store.baseUrl, store.apiKey) {
        if (store.apiKey.isNotBlank() && models.isEmpty()) loadModels()
    }

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

            // 主按钮：连接并获取模型（保留原来的按钮形式）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (loading) colors.surface2 else colors.primary)
                    .clickable(enabled = !loading) { loadModels() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        loading -> "正在获取模型…"
                        models.isEmpty() -> "连接并获取模型"
                        else -> "刷新模型列表"
                    },
                    style = DsText.button,
                    color = if (loading) colors.textSecondary else colors.onPrimary,
                )
            }

            status?.let { text ->
                Text(
                    text = text,
                    style = DsText.caption,
                    color = if (statusOk) colors.textSecondary else colors.danger,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }

            SectionLabel("模型")
            if (models.isEmpty()) {
                Text(
                    text = "填写 Key 后点击上方按钮，模型列表由供应商接口返回，不在本地写死。",
                    style = DsText.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            } else {
                SettingsCard {
                    models.forEachIndexed { index, model ->
                        ModelRow(
                            model = model,
                            selected = store.model == model.id,
                            onClick = { store.updateModel(model.id) },
                        )
                        if (index != models.lastIndex) Divider()
                    }
                }
            }

            Text(
                text = "Key 只保存在本机，用于调用你填写的 Base URL；本 App 不代理、不上传你的 Key。",
                style = DsText.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )

            // 语音识别：按住说话的转写走这个接口（OpenAI 兼容 /audio/transcriptions）
            SectionLabel("语音识别（可选）")
            SettingsCard {
                FieldRow(
                    title = "Base URL",
                    value = store.asrBaseUrl,
                    placeholder = "https://api.openai.com/v1",
                    masked = false,
                    onValueChange = store::updateAsrBaseUrl,
                )
                Divider()
                FieldRow(
                    title = "API Key",
                    value = store.asrApiKey,
                    placeholder = "留空复用上面的 Key",
                    masked = true,
                    onValueChange = store::updateAsrApiKey,
                )
                Divider()
                FieldRow(
                    title = "模型",
                    value = store.asrModel,
                    placeholder = SettingsStore.DEFAULT_ASR_MODEL,
                    masked = false,
                    onValueChange = store::updateAsrModel,
                )
            }
            Text(
                text = "填了这三项，「按住说话」录到的声音会发到该服务转成文字再回填输入框；留空则语音输入会提示未配置。",
                style = DsText.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
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
    model: ModelInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
        Column(Modifier.weight(1f)) {
            Text(
                text = model.name,
                style = DsText.label,
                color = colors.textPrimary,
            )
            Text(
                text = buildString {
                    append(model.id)
                    if (model.contextWindow > 0) {
                        append(" · 上下文 ")
                        append(formatTokens(model.contextWindow))
                    }
                    if (model.supportsImage) append(" · 支持图片")
                    if (model.effortLevels.isNotEmpty()) {
                        append(" · effort ")
                        append(model.effortLevels.joinToString("/"))
                    }
                },
                style = DsText.caption.copy(fontSize = 12.sp),
                color = colors.textSecondary,
            )
        }
    }
}

private fun formatTokens(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}K"
    else -> value.toString()
}
