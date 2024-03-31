package com.storyteller_f.file_system_ktx

import android.widget.ImageView
import com.storyteller_f.file_system.model.FileInfo

val FileInfo.isFile
    get() = kind.isFile

@Suppress("unused")
val FileInfo.isDirectory get() = kind.isDirectory

@Suppress("unused")
fun ImageView.fileIcon(fileInfo: FileInfo) {
    if (fileInfo.isFile) {
        if (fileInfo.fullPath.startsWith("/data/app/")) {
            setImageDrawable(context.packageManager.getApplicationIcon(fileInfo.name))
            return
        }
        val extension = fileInfo.extension!!
        if (extension.isNotEmpty()) {
            val placeholder = when (extension) {
                "mp3", "wav", "flac" -> R.drawable.ic_music
                "zip", "7z", "rar" -> R.drawable.ic_archive
                "jpg", "jpeg", "png", "gif" -> R.drawable.ic_image
                "mp4", "rmvb", "ts", "avi", "mkv", "3gp", "mov", "flv", "m4s" -> R.drawable.ic_video
                "url" -> R.drawable.ic_url
                "txt", "c" -> R.drawable.ic_text
                "js" -> R.drawable.ic_js
                "pdf" -> R.drawable.ic_pdf
                "doc", "docx" -> R.drawable.ic_word
                "xls", "xlsx" -> R.drawable.ic_excel
                "ppt", "pptx" -> R.drawable.ic_ppt
                "iso" -> R.drawable.ic_disk
                "exe", "msi" -> R.drawable.ic_exe
                "psd" -> R.drawable.ic_psd
                "torrent" -> R.drawable.ic_torrent
                else -> R.drawable.ic_unknow
            }
            setImageResource(placeholder)
        } else {
            setImageResource(R.drawable.ic_binary)
        }
    } else {
        setImageResource(R.drawable.ic_folder_explorer)
    }
}
