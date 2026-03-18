package com.storyteller_f.file_system_ktx

import android.net.Uri
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
import org.robolectric.Shadows.shadowOf

class MockImageView(context: android.content.Context) : ImageView(context) {
    var lastResId: Int = 0
    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        lastResId = resId
    }
}

@RunWith(RobolectricTestRunner::class)
class CommonTest {

    private fun mockFileInfo(name: String, path: String, isDir: Boolean = false, size: Long = 0, extension: String = ""): FileInfo {
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
}
