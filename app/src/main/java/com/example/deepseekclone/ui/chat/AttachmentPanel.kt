package com.example.deepseekclone.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deepseekclone.ui.theme.DsText
import com.example.deepseekclone.ui.theme.DsTheme

/**
 * 输入卡「+」展开的附件面板（对照官方 2.8 实测）：
 * 输入卡下方一整块，三个 114.7×84dp 圆角大方块并排（拍照 / 相册 / 文件），
 * 间距 8dp、左右 20dp，图标 20dp 在上、标签 15sp 在下。
 */
@Composable
fun AttachmentPanel(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onFile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PanelTile(
                icon = Icons.Outlined.PhotoCamera,
                label = "拍照",
                modifier = Modifier.weight(1f),
                onClick = onCamera,
            )
            PanelTile(
                icon = Icons.Outlined.Image,
                label = "相册",
                modifier = Modifier.weight(1f),
                onClick = onGallery,
            )
            PanelTile(
                icon = Icons.Outlined.AttachFile,
                label = "文件",
                modifier = Modifier.weight(1f),
                onClick = onFile,
            )
        }
    }
}

@Composable
private fun PanelTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = DsTheme.colors
    Column(
        modifier = modifier
            .height(84.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.tile)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textPrimary,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = label,
            style = DsText.body.copy(fontSize = 15.sp, lineHeight = 18.sp),
            color = colors.textPrimary,
        )
    }
}
