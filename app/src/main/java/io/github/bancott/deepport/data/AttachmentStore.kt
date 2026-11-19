package io.github.bancott.deepport.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.bancott.deepport.ui.chat.AttachmentUi
import java.io.File
import java.util.UUID

/**
 * 附件仓库：把相册 / 文件 / 相机拿到的内容统一复制到 App 私有目录，
 * 这样重启后还能读到（content:// 授权会失效），也便于把本地路径落盘。
 */
class AttachmentStore(context: Context) {

    private val app = context.applicationContext
    private val dir: File = File(app.filesDir, "attachments").apply { mkdirs() }

    /** 从 content:// 或 file:// 导入 */
    fun importUri(uri: Uri): AttachmentUi? {
        val resolver = app.contentResolver
        val name = queryDisplayName(uri) ?: "附件"
        val mime = resolver.getType(uri).orEmpty()
        val target = File(dir, uniqueName(name))
        return runCatching {
            val saved = resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } != null
            if (!saved) return null
            build(name = name, file = target, mime = mime)
        }.getOrNull()
    }

    /** 导入 App 自己生成的文件（相机拍摄 / 录音） */
    fun importFile(file: File, name: String = file.name): AttachmentUi? {
        if (!file.exists()) return null
        val target = File(dir, uniqueName(name))
        return runCatching {
            file.copyTo(target, overwrite = true)
            build(name = name, file = target, mime = null)
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun build(name: String, file: File, mime: String?): AttachmentUi {
        val isImage = when {
            !mime.isNullOrBlank() -> mime.startsWith("image/")
            else -> name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS
        }
        return AttachmentUi(
            id = "att-" + UUID.randomUUID().toString().take(8),
            displayName = name,
            isImage = isImage,
            path = file.absolutePath,
            sizeBytes = file.length(),
        )
    }

    private fun uniqueName(name: String): String {
        val safe = name.replace(Regex("[\\\\/:*?\"<>|\\s]"), "_").take(60).ifBlank { "file" }
        return UUID.randomUUID().toString().take(8) + "_" + safe
    }

    private fun queryDisplayName(uri: Uri): String? {
        runCatching {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()?.let { return it }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
    }

    companion object {
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif", "gif", "bmp")
    }
}
