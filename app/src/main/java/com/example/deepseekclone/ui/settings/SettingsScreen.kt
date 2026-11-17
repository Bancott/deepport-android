package com.example.deepseekclone.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 设置总览页。分组顺序与官方一致；
 * 「模型设置」「官方充值」是复刻版新增项，放在「账户」分组里。
 */
@Composable
fun SettingsScreen(
    versionName: String,
    modelLabel: String,
    isSystemDark: Boolean,
    onBack: () -> Unit,
    onAccount: () -> Unit = {},
    onData: () -> Unit = {},
    onModel: () -> Unit = {},
    onRecharge: () -> Unit = {},
    onLanguage: () -> Unit = {},
    onAppearance: () -> Unit = {},
    onFontSize: () -> Unit = {},
    onPersonalization: () -> Unit = {},
    onVoiceTone: () -> Unit = {},
    onCheckUpdate: () -> Unit = {},
    onAgreement: () -> Unit = {},
    onHelp: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bgGrouped)
            .statusBarsPadding(),
    ) {
        // 顶栏：圆形返回 + 居中标题
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
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
                text = "设置",
                style = DsText.h2.copy(fontSize = 18.sp),
                color = colors.textPrimary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SectionLabel("账户")
            SettingsGroup {
                SettingsRow(Icons.Outlined.PersonOutline, "账号管理", onClick = onAccount)
                SettingsRow(Icons.Outlined.Storage, "数据管理", onClick = onData)
                SettingsRow(Icons.Outlined.Tune, "模型设置", value = modelLabel, onClick = onModel)
                SettingsRow(Icons.Outlined.Info, "DeepSeek 官方充值", value = "官网", onClick = onRecharge)
            }

            SectionLabel("应用")
            SettingsGroup {
                SettingsRow(Icons.Outlined.Language, "语言", value = "中文（简体中文）", onClick = onLanguage)
                SettingsRow(
                    icon = if (isSystemDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    title = "外观",
                    value = "跟随系统",
                    onClick = onAppearance,
                )
                SettingsRow(Icons.Outlined.FormatSize, "字体大小", onClick = onFontSize)
                SettingsRow(Icons.Outlined.AutoAwesome, "个性化", onClick = onPersonalization)
            }

            SectionLabel("语音")
            SettingsGroup {
                SettingsRow(Icons.Outlined.VolumeUp, "朗读音色", onClick = onVoiceTone)
            }

            SectionLabel("关于")
            SettingsGroup {
                SettingsRow(Icons.Outlined.Info, "检查更新", value = versionName, onClick = onCheckUpdate)
                SettingsRow(Icons.Outlined.Description, "服务协议", onClick = onAgreement)
            }

            Spacer(Modifier.height(20.dp))
            SettingsGroup {
                SettingsRow(Icons.Outlined.HelpOutline, "帮助与反馈", onClick = onHelp)
            }

            Spacer(Modifier.height(20.dp))
            SettingsGroup {
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "退出登录",
                    onClick = onLogout,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = DsTheme.colors
    Text(
        text = text,
        style = DsText.caption,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    val colors = DsTheme.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface2,
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = DsText.label,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = DsText.label.copy(fontSize = 15.sp),
                color = colors.textSecondary,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(20.dp),
        )
    }
}



