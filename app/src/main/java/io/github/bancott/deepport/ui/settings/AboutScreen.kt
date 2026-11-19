package io.github.bancott.deepport.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import io.github.bancott.deepport.ui.theme.DeepSeekTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

/**
 * 「关于」页：应用标识、版本、入口（项目主页 / 帮助与反馈 / 服务协议 / 开源许可）与免责声明。
 * 视觉沿用设置子页（分组标题 + 圆角卡片），不引入新的设计语言。
 */
@Composable
fun AboutScreen(
    versionName: String,
    darkTheme: Boolean,
    onBack: () -> Unit,
    onOpenGitHub: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAgreement: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    val colors = DsTheme.colors
    var showLicense by remember { mutableStateOf(false) }
    SettingsSubPage("关于", onBack) {
        // 顶部标识区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                SvgIcon(
                    R.drawable.ic_deepseek_whale,
                    tint = colors.textPrimary,
                    size = 44.dp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "DeepPort",
                style = DsText.h2.copy(fontSize = 22.sp),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "版本 $versionName",
                style = DsText.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "第三方 DeepSeek 客户端",
                style = DsText.caption.copy(fontSize = 13.sp),
                color = colors.textTertiary,
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup {
            SettingsRow(Icons.Outlined.OpenInNew, "项目主页", value = "GitHub", onClick = onOpenGitHub)
            SettingsRow(Icons.Outlined.HelpOutline, "帮助与反馈", onClick = onOpenHelp)
            SettingsRow(Icons.Outlined.Description, "服务协议", onClick = onOpenAgreement)
            SettingsRow(Icons.Outlined.Gavel, "开源许可", value = "MIT", onClick = { showLicense = true })
        }

        Spacer(Modifier.height(12.dp))
        SettingsGroup {
            SettingsRow(
                Icons.Outlined.Refresh,
                "检查更新",
                value = versionName,
                onClick = onCheckUpdate,
            )
        }

        Spacer(Modifier.height(20.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = colors.surface2,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "免责声明",
                    style = DsText.label.copy(fontSize = 15.sp),
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "本应用由第三方开发者制作，基于 DeepSeek 开放 API 构建，并非 DeepSeek 官方产品，" +
                        "也不代表官方立场。应用本身不收费，调用模型需要你自备 API Key（由服务商按量计费）。\n\n" +
                        "本应用不收集个人信息，对话与 API Key 仅保存在本机，只用于向你设置的 API 服务发起请求。",
                    style = DsText.body.copy(fontSize = 14.sp, lineHeight = 22.sp),
                    color = colors.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "© 2026 Bancott · MIT License",
            style = DsText.caption.copy(fontSize = 12.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }

    if (showLicense) {
        LicenseDialog(darkTheme = darkTheme, onDismiss = { showLicense = false })
    }
}

/** 许可说明：代码 MIT，官方商标/资源不属于许可范围，第三方库单独列出 */
private const val LICENSE_TEXT = """本应用源码以 MIT 许可证发布：可自由使用、修改、分发与商用，但需保留版权声明和许可声明；作者不对使用后果承担责任。

MIT 许可证不涵盖 DeepSeek 的名称、商标、图标及官方资源，这些权利归杭州深度求索人工智能基础技术研究有限公司所有。本应用为非官方第三方客户端，与官方无隶属关系。

第三方组件：
· Jetpack Compose / AndroidX（Apache-2.0）
· Room（Apache-2.0）
· Lottie for Android（Apache-2.0）
· AndroidX Browser（Apache-2.0）

完整许可文本见仓库 LICENSE 文件。"""

@Composable
private fun LicenseDialog(darkTheme: Boolean, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        DeepSeekTheme(darkTheme = darkTheme) {
            val colors = DsTheme.colors
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.dialog),
            ) {
                Text(
                    text = "开源许可",
                    style = DsText.h2.copy(fontSize = 19.sp),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 14.dp),
                )
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                ) {
                    Text(
                        text = LICENSE_TEXT,
                        style = DsText.body.copy(fontSize = 14.sp, lineHeight = 22.sp),
                        color = colors.textSecondary,
                    )
                }
                Text(
                    text = "关闭",
                    style = DsText.button.copy(fontSize = 16.sp),
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 14.dp),
                )
            }
        }
    }
}
