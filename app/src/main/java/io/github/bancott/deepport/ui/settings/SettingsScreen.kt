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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import io.github.bancott.deepport.BuildConfig
import io.github.bancott.deepport.data.UpdateChecker
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
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = DsTheme.colors
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateRelease by remember { mutableStateOf<UpdateChecker.Release?>(null) }
    var checking by remember { mutableStateOf(false) }
    var updateFailed by remember { mutableStateOf(false) }

    fun checkUpdate(manual: Boolean) {
        if (checking) return
        checking = true
        updateFailed = false
        scope.launch {
            val result = UpdateChecker.latest()
            checking = false
            result.onSuccess { release ->
                if (UpdateChecker.isNewer(release.version, BuildConfig.VERSION_NAME)) {
                    updateRelease = release
                } else if (manual) {
                    android.widget.Toast.makeText(ctx, "已是最新版本", android.widget.Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                updateFailed = true
                if (manual) {
                    android.widget.Toast.makeText(ctx, "检查更新失败，请检查网络", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    LaunchedEffect(Unit) { checkUpdate(false) }

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
                SettingsRow(
                    Icons.Outlined.Info,
                    "关于 DeepPort",
                    value = "v${BuildConfig.VERSION_NAME}",
                    onClick = onAbout,
                )
                SettingsRow(
                    R.drawable.ic_download_outline_20,
                    "检查更新",
                    value = when {
                        checking -> "检查中…"
                        updateRelease != null -> "发现 v${updateRelease?.version.orEmpty()}"
                        updateFailed -> "检查失败"
                        else -> "已是最新"
                    },
                    onClick = { checkUpdate(true) },
                )
            }

            // 退出登录仍不显示：这个壳没有官方账号会话，放一个按了没反应的入口比没有更糟。
        }
    }

    updateRelease?.let { release ->
        AlertDialog(
            onDismissRequest = { updateRelease = null },
            title = { Text("发现新版本 v${release.version}", color = colors.textPrimary) },
            text = {
                Text(
                    text = release.notes.ifBlank { "新版本已发布，建议下载更新。" }.take(400),
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        runCatching {
                            ctx.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(
                                        release.url.ifBlank {
                                            "https://github.com/Bancott/deepport-android/releases/latest"
                                        },
                                    ),
                                ),
                            )
                        }
                        updateRelease = null
                    },
                ) {
                    Text("去下载", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { updateRelease = null }) {
                    Text("稍后", color = colors.textSecondary)
                }
            },
            containerColor = colors.dialog,
        )
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
) = SettingsRowShell(
    title = title,
    value = value,
    onClick = onClick,
    icon = {
        SvgIcon(iconRes, tint = DsTheme.colors.textPrimary, size = 22.dp)
    },
)

/** 同一个行样式，图标改用 Material ImageVector：跨图标视觉尺寸一致（关于页用） */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) = SettingsRowShell(
    title = title,
    value = value,
    onClick = onClick,
    icon = {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DsTheme.colors.textPrimary,
            modifier = Modifier.size(22.dp),
        )
    },
)

@Composable
private fun SettingsRowShell(
    title: String,
    value: String?,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
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
        icon()
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






