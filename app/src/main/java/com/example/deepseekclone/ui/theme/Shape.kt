package com.example.deepseekclone.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 圆角 Token：8 / 12 / 16 / 20 / 24 / 28 */
val DsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

object DsDimen {
    val TopBarHeight = 56.dp
    val IconSize = 24.dp
    val IconButtonSize = 40.dp
    val TouchTarget = 48.dp

    val PagePadding = 24.dp
    val CardPadding = 16.dp

    /** 输入卡左右外边距 */
    val InputHorizontalPadding = 12.dp
    val InputCornerRadius = 28.dp

    val ChipHeight = 32.dp

    /** 用户气泡 */
    val BubbleMaxWidthFraction = 0.78f
    val BubbleCornerRadius = 20.dp
}
