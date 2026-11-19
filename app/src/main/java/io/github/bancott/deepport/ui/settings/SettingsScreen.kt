package io.github.bancott.deepport.ui.settings

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
import androidx.annotation.DrawableRes
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.CircleIconButton
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme

/**
 * 设置总览页。分组顺序与官方一致；
 * 「模型设置」「官方充值」是复刻版新增项，放在「账户」分组里。
 */
@Composable
fun SettingsScreen(
    versionName: String,
    modelLabel: String,
    themeLabel: String = "跟随系统",
    isDarkTheme: Boolean,
    voiceLabel: String = "海星",
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
    onAbout: () -> Unit = {},
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
                SvgIcon(
                    R.drawable.ic_chevron_left_outline_20,
                    tint = colors.textPrimary,
                    size = 20.dp,
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
                SettingsRow(R.drawable.ic_user_regular, "账号管理", onClick = onAccount)
                SettingsRow(R.drawable.ic_data_regular, "数据管理", onClick = onData)
                // 这两项是复刻版新增：官方壳不接官方账号体系，但模型 Key 必须有地方配。
                SettingsRow(R.drawable.ic_enhance_outline_20, "模型设置", value = modelLabel, onClick = onModel)
                SettingsRow(R.drawable.ic_redirect_regular, "DeepSeek 官方充值", value = "官网", onClick = onRecharge)
            }

            SectionLabel("应用")
            SettingsGroup {
                SettingsRow(R.drawable.ic_globe_language_regular, "语言", value = "中文（简体中文）", onClick = onLanguage)
                SettingsRow(
                    iconRes = if (isDarkTheme) R.drawable.ic_moon_regular else R.drawable.ic_sun_regular,
                    title = "外观",
                    value = themeLabel,
                    onClick = onAppearance,
                )
                SettingsRow(R.drawable.ic_size_regular, "字体大小", onClick = onFontSize)
                SettingsRow(R.drawable.ic_wand, "个性化", onClick = onPersonalization)
            }

            SectionLabel("语音")
            SettingsGroup {
                SettingsRow(R.drawable.ic_play_media_outline_20, "朗读音色", value = voiceLabel, onClick = onVoiceTone)
            }

            SectionLabel("关于")
            SettingsGroup {
                SettingsRow(R.drawable.ic_info_outline, "检查更新", value = versionName, onClick = onCheckUpdate)
                SettingsRow(R.drawable.ic_list_pen_regular, "服务协议", onClick = onAgreement)
                SettingsRow(R.drawable.ic_info_outline_16, "关于 DeepPort", onClick = onAbout)
            }

            Spacer(Modifier.height(12.dp))
            SettingsGroup {
                SettingsRow(R.drawable.ic_help_outline, "帮助与反馈", onClick = onHelp)
            }

            // 退出登录仍不显示：这个壳没有官方账号会话，放一个按了没反应的入口比没有更糟。
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    val colors = DsTheme.colors
    Text(
        text = text,
        style = DsText.caption,
        color = colors.textSecondary,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
internal fun SettingsGroup(content: @Composable () -> Unit) {
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
internal fun SettingsRow(
    @DrawableRes iconRes: Int,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SvgIcon(iconRes, tint = colors.textPrimary, size = 22.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = DsText.label.copy(fontSize = 16.sp),
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
        SvgIcon(
            R.drawable.ic_chevron_right_outline_20,
            tint = colors.textTertiary,
            size = 20.dp,
        )
    }
}






