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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/** 设置类子页的统一骨架：返回 + 标题 + 可滚动内容 */
@Composable
fun SettingsSubPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScopeAlias.() -> Unit,
) {
    val colors = DsTheme.colors
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
                text = title,
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
            content(ColumnScopeAlias)
        }
    }
}

/** 仅用于给内容块一个接收者，避免额外 import ColumnScope 冲突 */
object ColumnScopeAlias

@Composable
fun SubSectionLabel(text: String) {
    Text(
        text = text,
        style = DsText.caption,
        color = DsTheme.colors.textSecondary,
        modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
fun SubCard(content: @Composable () -> Unit) {
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
fun SubDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(1.dp)
            .background(DsTheme.colors.outline),
    )
}

@Composable
fun SubRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = DsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = DsText.label.copy(fontSize = 15.sp), color = colors.textPrimary)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = DsText.caption.copy(fontSize = 12.sp),
                    color = colors.textSecondary,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = DsText.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary,
            )
            Spacer(Modifier.width(8.dp))
        }
        trailing?.invoke()
    }
}

@Composable
fun SubSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SubRow(
        title = title,
        subtitle = subtitle,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = DsTheme.colors.onPrimary,
                    checkedTrackColor = DsTheme.colors.primary,
                ),
            )
        },
    )
}

/* ------------------------------ 账号管理 ------------------------------ */

@Composable
fun AccountScreen(onBack: () -> Unit, userName: String = "Li .chu .kun") {
    val colors = DsTheme.colors
    var wechatBound by remember { mutableStateOf(false) }
    SettingsSubPage("账号管理", onBack) {
        SubSectionLabel("账号")
        SubCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(colors.surface3),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("L", style = DsText.h2, color = colors.textPrimary)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(userName, style = DsText.label.copy(fontSize = 16.sp), color = colors.textPrimary)
                    Text(
                        "未绑定手机号",
                        style = DsText.caption.copy(fontSize = 12.sp),
                        color = colors.textSecondary,
                    )
                }
            }
            SubDivider()
            SubRow("昵称", value = userName)
            SubDivider()
            SubRow("手机号", value = "去绑定")
        }

        SubSectionLabel("第三方账号")
        SubCard {
            SubRow(
                title = "微信",
                subtitle = if (wechatBound) "已绑定，可直接登录" else "绑定后可用微信登录 DeepSeek",
                value = if (wechatBound) "已绑定" else "去绑定",
                onClick = { wechatBound = !wechatBound },
            )
        }

        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Text("退出登录", style = DsText.button, color = colors.danger)
        }

        Text(
            text = "退出登录后，本机会话记录仍保留；重新登录可同步云端会话。",
            style = DsText.caption.copy(fontSize = 12.sp),
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}

/* ------------------------------ 数据管理 ------------------------------ */

@Composable
fun DataScreen(
    onBack: () -> Unit,
    conversationCount: Int = 13,
    onClearAll: () -> Unit = {},
) {
    val colors = DsTheme.colors
    var cacheMb by remember { mutableFloatStateOf(18.6f) }
    SettingsSubPage("数据管理", onBack) {
        SubSectionLabel("会话")
        SubCard {
            SubRow("会话数量", value = "$conversationCount 个")
            SubDivider()
            SubRow(
                "清空全部会话",
                subtitle = "删除后不可恢复",
                onClick = {
                    onClearAll()
                    cacheMb = cacheMb
                },
            )
        }

        SubSectionLabel("存储")
        SubCard {
            SubRow("本地缓存", value = "%.1f MB".format(cacheMb), onClick = { cacheMb = 0.4f })
            SubDivider()
            SubRow("导出会话", subtitle = "导出为 Markdown 文件", value = "导出", onClick = { })
        }

        Text(
            text = "会话默认只保存在本机。开启云端同步后才会写入账号。",
            style = DsText.caption.copy(fontSize = 12.sp),
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}

/* ------------------------------ 个性化 ------------------------------ */

@Composable
fun PersonalizationScreen(
    onBack: () -> Unit,
    effortLevels: List<String> = listOf("low", "high", "max"),
    defaultEffort: String = "high",
) {
    val colors = DsTheme.colors
    var autoCollapse by remember { mutableStateOf(true) }
    var effort by remember { mutableStateOf(defaultEffort) }
    var role by remember { mutableStateOf("通用助手") }
    val roles = listOf("通用助手", "严谨学者", "编程搭档", "口语陪练")

    SettingsSubPage("个性化", onBack) {
        SubSectionLabel("思考")
        SubCard {
            SubSwitchRow(
                title = "思考过程自动折叠",
                subtitle = "回答开始输出后收起推理内容",
                checked = autoCollapse,
                onCheckedChange = { autoCollapse = it },
            )
            SubDivider()
            SubRow("思考强度", subtitle = "影响推理长度与耗时", value = effort)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            effortLevels.forEach { level ->
                val selected = effort == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) colors.primary.copy(alpha = 0.16f) else colors.surface2)
                        .border(
                            width = 1.dp,
                            color = if (selected) colors.primary.copy(alpha = 0.6f) else colors.outline,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { effort = level },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = level,
                        style = DsText.caption.copy(fontSize = 13.sp),
                        color = if (selected) colors.primary else colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        SubSectionLabel("角色")
        SubCard {
            roles.forEachIndexed { index, r ->
                SubRow(
                    title = r,
                    value = if (role == r) "当前" else null,
                    onClick = { role = r },
                )
                if (index != roles.lastIndex) SubDivider()
            }
        }

        Text(
            text = "思考强度对应接口返回的 effort 档位；角色只影响系统提示词。",
            style = DsText.caption.copy(fontSize = 12.sp),
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}
