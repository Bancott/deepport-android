package io.github.bancott.deepport.ui.chat

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.bancott.deepport.R
import io.github.bancott.deepport.ui.common.SvgIcon
import io.github.bancott.deepport.ui.theme.DsText
import io.github.bancott.deepport.ui.theme.DsTheme
import java.io.File

/** 官方 2.8 的文件类型图标；未知后缀回退到文本文件 */
private fun fileTypeIcon(name: String): Int = when (name.substringAfterLast('.', "").lowercase()) {
    "pdf" -> R.drawable.ic_type_pdf
    "xls", "xlsx", "csv" -> R.drawable.ic_type_xls
    "png", "jpg", "jpeg", "webp", "gif", "heic", "bmp" -> R.drawable.ic_type_pic
    else -> R.drawable.ic_type_text
}

/**
 * 本地图片缩略图：按目标尺寸采样解码，避免大图直接进内存。
 * path 为 null 或解码失败时退化为占位块。
 */
@Composable
fun LocalImage(
    path: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    val colors = DsTheme.colors
    val bitmap = remember(path) { path?.let { decodeSampled(it, 900) } }
    val shape = RoundedCornerShape(cornerRadius)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            SvgIcon(
                R.drawable.ic_photo_error_filled,
                tint = colors.textTertiary,
                size = 20.dp,
            )
        }
    }
}

private fun decodeSampled(path: String, requested: Int): androidx.compose.ui.graphics.ImageBitmap? {
    val file = File(path)
    if (!file.exists()) return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= requested || bounds.outHeight / (sample * 2) >= requested) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return runCatching { BitmapFactory.decodeFile(path, options)?.asImageBitmap() }.getOrNull()
}

/** 输入卡里的待发送附件：图片缩略图 / 文件 chip，带删除角标 */
@Composable
fun PendingAttachmentRow(
    items: List<AttachmentUi>,
    onRemove: (AttachmentUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, top = 2.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.take(3).forEach { item ->
            Box {
                if (item.isImage) {
                    LocalImage(
                        path = item.path,
                        modifier = Modifier.size(56.dp),
                        cornerRadius = 10.dp,
                    )
                } else {
                    Box(Modifier.size(56.dp)) {
                        FileAttachmentTile(item = item, compact = true, modifier = Modifier.size(56.dp))
                    }
                }
                RemoveBadge(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = { onRemove(item) },
                )
            }
        }
        if (items.size > 3) {
            Text(
                text = "+${items.size - 3}",
                style = DsText.caption.copy(fontSize = 14.sp),
                color = DsTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun RemoveBadge(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = DsTheme.colors
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(colors.bg.copy(alpha = 0.72f))
            .clickable(role = Role.Button, onClickLabel = "移除附件", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SvgIcon(
            R.drawable.ic_close_outline_16,
            tint = colors.textPrimary,
            size = 12.dp,
        )
    }
}

/** 文件类型附件：圆角方块 + 文件名 + 大小 */
@Composable
fun FileAttachmentTile(
    item: AttachmentUi,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = DsTheme.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface2),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SvgIcon(
                fileTypeIcon(item.displayName),
                tint = colors.textSecondary,
                size = if (compact) 20.dp else 22.dp,
            )
            if (compact) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.displayName.substringAfterLast('.', "").uppercase().take(4),
                    style = DsText.meta.copy(fontSize = 9.sp),
                    color = colors.textTertiary,
                )
            }
        }
    }
}

/** 已发送消息里的附件：图片直接铺开，文件是一行 chip */
@Composable
fun MessageAttachments(items: List<AttachmentUi>) {
    if (items.isEmpty()) return
    val colors = DsTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        items.take(3).forEach { item ->
            if (item.isImage) {
                LocalImage(
                    path = item.path,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .height(150.dp)
                        .width(150.dp),
                    cornerRadius = 14.dp,
                )
                Spacer(Modifier.height(6.dp))
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.bubble)
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SvgIcon(
                        fileTypeIcon(item.displayName),
                        tint = colors.textSecondary,
                        size = 18.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.displayName,
                        style = DsText.caption.copy(fontSize = 13.sp),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp),
                    )
                    if (item.sizeLabel.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.sizeLabel,
                            style = DsText.meta.copy(fontSize = 11.sp),
                            color = colors.textTertiary,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
