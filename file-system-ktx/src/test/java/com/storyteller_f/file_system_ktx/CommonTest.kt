package com.storyteller_f.file_system_ktx

import android.net.Uri
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class MockImageView(context: android.content.Context) : ImageView(context) {
    var lastResId: Int = 0
    var lastDrawable: Drawable? = null
    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        lastResId = resId
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        lastDrawable = drawable
    }
}

@RunWith(RobolectricTestRunner::class)
class CommonTest {

    private fun mockFileInfo(
        name: String,
        path: String,
        isDir: Boolean = false,
        size: Long = 0,
        extension: String = ""
    ): FileInfo {
        val kind = if (isDir) {
            FileKind.Directory(null, false)
        } else {
            FileKind.File(null, false, size, extension)
        }
        val uri = Uri.Builder().path(path).build()
        return FileInfo(name, uri, FileTime(), kind, FilePermissions.USER_READABLE)
    }

    @Test
    fun testIsFileAndIsDirectory() {
        val fileInfoFile = mockFileInfo("test.txt", "/test.txt", isDir = false, extension = "txt")
        assertTrue(fileInfoFile.isFile)

        val fileInfoDir = mockFileInfo("test_dir", "/test_dir", isDir = true)
        assertTrue(fileInfoDir.isDirectory)
    }

    @Test
    fun testFileIconAssignment() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val imageView = MockImageView(context)

        // Test Word Document
        val documentDoc = mockFileInfo("word.doc", "/word.doc", isDir = false, extension = "doc")
        imageView.fileIcon(documentDoc)
        assertEquals(R.drawable.ic_word, imageView.lastResId)

        // Test Folder
        val folder = mockFileInfo("folder", "/folder", isDir = true)
        imageView.fileIcon(folder)
        assertEquals(R.drawable.ic_folder_explorer, imageView.lastResId)

        // Test Image
        val image = mockFileInfo("pic.png", "/pic.png", isDir = false, extension = "png")
        imageView.fileIcon(image)
        assertEquals(R.drawable.ic_image, imageView.lastResId)

        // Test Unknown Ext
        val unknown = mockFileInfo("file.xyz", "/file.xyz", isDir = false, extension = "xyz")
        imageView.fileIcon(unknown)
        assertEquals(R.drawable.ic_unknow, imageView.lastResId)

        // Test Empty Ext (binary)
        val binary = mockFileInfo("Makefile", "/Makefile", isDir = false, extension = "")
        imageView.fileIcon(binary)
        assertEquals(R.drawable.ic_binary, imageView.lastResId)
    }

    @Test
    fun testFileIconAssignmentsForKnownExtensions() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val imageView = MockImageView(context)

        val cases = listOf(
            "song.mp3" to R.drawable.ic_music,
            "sound.wav" to R.drawable.ic_music,
            "audio.flac" to R.drawable.ic_music,
            "archive.zip" to R.drawable.ic_archive,
            "archive.7z" to R.drawable.ic_archive,
            "archive.rar" to R.drawable.ic_archive,
            "photo.jpg" to R.drawable.ic_image,
            "photo.jpeg" to R.drawable.ic_image,
            "photo.gif" to R.drawable.ic_image,
            "movie.mp4" to R.drawable.ic_video,
            "movie.rmvb" to R.drawable.ic_video,
            "movie.ts" to R.drawable.ic_video,
            "movie.avi" to R.drawable.ic_video,
            "movie.mkv" to R.drawable.ic_video,
            "movie.3gp" to R.drawable.ic_video,
            "movie.mov" to R.drawable.ic_video,
            "movie.flv" to R.drawable.ic_video,
            "movie.m4s" to R.drawable.ic_video,
            "bookmark.url" to R.drawable.ic_url,
            "notes.txt" to R.drawable.ic_text,
            "source.c" to R.drawable.ic_text,
            "script.js" to R.drawable.ic_js,
            "paper.pdf" to R.drawable.ic_pdf,
            "sheet.xls" to R.drawable.ic_excel,
            "sheet.xlsx" to R.drawable.ic_excel,
            "slides.ppt" to R.drawable.ic_ppt,
            "slides.pptx" to R.drawable.ic_ppt,
            "disk.iso" to R.drawable.ic_disk,
            "program.exe" to R.drawable.ic_exe,
            "installer.msi" to R.drawable.ic_exe,
            "design.psd" to R.drawable.ic_psd,
            "download.torrent" to R.drawable.ic_torrent,
        )

        cases.forEach { (name, icon) ->
            imageView.fileIcon(mockFileInfo(name, "/$name", extension = name.substringAfterLast('.')))
            assertEquals(name, icon, imageView.lastResId)
        }
    }

    @Test
    fun testFileIconUsesApplicationIconForInstalledAppPath() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val imageView = MockImageView(context)
        val packageName = context.packageName

        imageView.fileIcon(
            mockFileInfo(
                name = packageName,
                path = "/data/app/$packageName",
                extension = ""
            )
        )

        assertEquals(0, imageView.lastResId)
    }
}
