package com.example.deepseekclone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.Default

/** 映射到 Material3 的槽位，保证 M3 组件（Switch/Slider/Dialog）风格一致 */
val DsTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 14.sp,
    ),
)

/**
 * 设计 Token 的正文字号表。页面里只用这里的样式，不写裸 fontSize。
 */
object DsText {
    /** 空态问候 */
    val display = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp)

    /** 顶栏标题 */
    val titleBar = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 24.sp)

    /** Markdown 一、二级标题 */
    val h1 = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp)

    /** Markdown 三级标题 */
    val h2 = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp)

    /** Markdown 正文 */
    val body = TextStyle(fontSize = 17.sp, lineHeight = 28.sp)

    /** 气泡 / 推理正文 */
    val bodyTight = TextStyle(fontSize = 17.sp, lineHeight = 26.sp)

    /** 设置行、抽屉行 */
    val label = TextStyle(fontSize = 17.sp, lineHeight = 24.sp)

    /** 说明文字、免责声明 */
    val caption = TextStyle(fontSize = 14.sp, lineHeight = 20.sp)

    /** token 用量 */
    val meta = TextStyle(fontSize = 11.5.sp, lineHeight = 14.sp)

    /** 主按钮 */
    val button = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp)
}
