package com.example.deepseekclone.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * DeepSeek 复刻版语义色板。
 * 所有颜色都来自对官方 2.8.0 截图的取色（小米 14 / 1200x2670 / 3.0x），
 * 禁止在页面里写死 Hex，一律走这里。
 */
@Immutable
data class DsColors(
    /** 页面底色（聊天页） */
    val bg: Color,
    /** 分组列表页底色（设置、抽屉） */
    val bgGrouped: Color,
    /** 一级表面：输入卡 */
    val surface1: Color,
    /** 二级表面：设置卡、Sheet、来源列表 */
    val surface2: Color,
    /** 三级表面：弹窗、气泡、序号徽标 */
    val surface3: Color,
    /** 用户气泡 */
    val bubble: Color,
    /** 功能 chip 底（= 主色 12%） */
    val chipBg: Color,
    /** 功能 chip 前景（深色下比主色更亮一档） */
    val chipContent: Color,
    /** 1dp 描边 / 分隔线 */
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val danger: Color,
    val warning: Color,
    val switchOn: Color,
    val toast: Color,
    /** 抽屉面板底 */
    val drawerSurface: Color,
    /** 抽屉推开后，下层页面被提亮的表面色 */
    val pushedPage: Color,
)

val DsLightColors = DsColors(
    bg = Color(0xFFFFFFFF),
    bgGrouped = Color(0xFFF8F8F8),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFFFFFFF),
    surface3 = Color(0xFFFFFFFF),
    bubble = Color(0xFFEFF3FE),
    chipBg = Color(0xFFEFF3FE),
    chipContent = Color(0xFF4C6EF6),
    outline = Color(0xFFEFEFF2),
    textPrimary = Color(0xFF0F0F0F),
    textSecondary = Color(0xFF8C9094),
    textTertiary = Color(0xFFB4B8BF),
    primary = Color(0xFF4C6EF6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFF3FE),
    danger = Color(0xFFE06560),
    warning = Color(0xFFE9A13B),
    switchOn = Color(0xFF5CC368),
    toast = Color(0xFF3C3C3E),
    drawerSurface = Color(0xFFF8F8F8),
    pushedPage = Color(0xFFFFFFFF),
)

val DsDarkColors = DsColors(
    bg = Color(0xFF0F0F0F),
    bgGrouped = Color(0xFF1E1E1E),
    surface1 = Color(0xFF242424),
    surface2 = Color(0xFF292929),
    surface3 = Color(0xFF333333),
    bubble = Color(0xFF333333),
    chipBg = Color(0xFF2B3242),
    chipContent = Color(0xFF7A9AF3),
    outline = Color(0xFF333333),
    textPrimary = Color(0xFFF8F8F8),
    textSecondary = Color(0xFF8C9094),
    textTertiary = Color(0xFF6E6E73),
    primary = Color(0xFF5A7AE9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF2B3242),
    danger = Color(0xFFE06560),
    warning = Color(0xFFE9A13B),
    switchOn = Color(0xFF5CC368),
    toast = Color(0xFF47484A),
    drawerSurface = Color(0xFF0F0F0F),
    pushedPage = Color(0xFF1D1D1D),
)

