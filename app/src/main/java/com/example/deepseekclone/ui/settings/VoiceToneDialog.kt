package com.example.deepseekclone.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.deepseekclone.R
import com.example.deepseekclone.ui.common.CircleIconButton
import com.example.deepseekclone.ui.common.SvgIcon
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme
import kotlinx.coroutines.launch

private data class VoiceTone(
    val id: String,
    val name: String,
    val subtitle: String,
    /** 卡片柔光的两档品牌色，靠 alpha 叠出"云"的感觉 */
    val glow: Boolean,
)

/**
 * 官方四档朗读音色（与 App 内顺序一致）：
 * 贝壳 / 白浪 / 海星 / 暗潮，接口 id 分别对应 mira / echo / stella / tide。
 */
private val VoiceTones = listOf(
    VoiceTone(id = "mira", name = "贝壳", subtitle = "百变活泼", glow = false),
    VoiceTone(id = "echo", name = "白浪", subtitle = "明朗坚定", glow = true),
    VoiceTone(id = "stella", name = "海星", subtitle = "俏皮甜美", glow = false),
    VoiceTone(id = "tide", name = "暗潮", subtitle = "低沉浑厚", glow = true),
)

/**
 * 朗读音色选择（底部弹层）：横向滑动卡片 + 圆点 + 确认。
 * 与官方「朗读音色」弹窗同样的结构。
 */
@Composable
fun VoiceToneDialog(
    selected: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = DsTheme.colors
    val initial = VoiceTones.indexOfFirst { it.id == selected }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = initial) { VoiceTones.size }
    var draft by remember(selected) { mutableStateOf(VoiceTones.getOrNull(initial)?.id ?: "stella") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pager.currentPage) {
        VoiceTones.getOrNull(pager.currentPage)?.let { draft = it.id }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 96.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = colors.dialog,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                    ) {
                        Text(
                            text = "朗读音色",
                            style = DsText.h2.copy(fontSize = 18.sp),
                            color = colors.textPrimary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        CircleIconButton(
                            contentDescription = "关闭",
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 12.dp),
                            containerColor = colors.surface3,
                        ) {
                            SvgIcon(
                                R.drawable.ic_close_outline_20,
                                tint = colors.textPrimary,
                                size = 20.dp,
                            )
                        }
                    }

                    HorizontalPager(
                        state = pager,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentPadding = PaddingValues(horizontal = 56.dp),
                        pageSpacing = 14.dp,
                    ) { page ->
                        VoiceCard(voice = VoiceTones[page])
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = VoiceTones.firstOrNull { it.id == draft }?.name.orEmpty(),
                        style = DsText.h2.copy(fontSize = 19.sp),
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = VoiceTones.firstOrNull { it.id == draft }?.subtitle.orEmpty(),
                        style = DsText.caption.copy(fontSize = 13.sp),
                        color = colors.textSecondary,
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceTones.forEachIndexed { index, _ ->
                            val active = index == pager.currentPage
                            Box(
                                modifier = Modifier
                                    .size(if (active) 7.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) colors.primary else colors.textTertiary)
                                    .clickable(role = Role.Button) {
                                        scope.launch { pager.animateScrollToPage(index) }
                                    },
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(colors.primary)
                            .clickable(role = Role.Button) {
                                onConfirm(draft)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "确认",
                            style = DsText.button,
                            color = colors.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceCard(voice: VoiceTone) {
    val colors = DsTheme.colors
    val topGlow = if (voice.glow) {
        colors.primary.copy(alpha = 0.55f)
    } else {
        colors.onPrimary.copy(alpha = 0.95f)
    }
    val bottomGlow = if (voice.glow) {
        colors.primary.copy(alpha = 0.30f)
    } else {
        colors.primary.copy(alpha = 0.28f)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(55.dp))
            .background(colors.voiceCard),
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .offset(x = 18.dp, y = (-48).dp)
                .background(
                    Brush.radialGradient(
                        listOf(topGlow, Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 18.dp, y = 30.dp)
                .background(
                    Brush.radialGradient(
                        listOf(bottomGlow, Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
    }
}
