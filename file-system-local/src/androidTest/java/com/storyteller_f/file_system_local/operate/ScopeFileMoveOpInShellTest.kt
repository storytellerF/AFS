package com.storyteller_f.file_system_local.operate

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.operate.ScopeFileMoveOpInShell
import com.storyteller_f.file_system.toChildEfficiently
import com.storyteller_f.file_system_local.getCurrentUserDataPath
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScopeFileMoveOpInShellTest {
    @Test
    fun shellMoveFileInUserPackage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val currentUserDataPath = context.getCurrentUserDataPath()
        val userDataUri = File(currentUserDataPath).toUri()

        runBlocking {
            val userData = getFileInstance(context, userDataUri)!!
            val pack = userData.list()
            val userPackageModel = pack.directories.first()
            val userPackage = userData.toChildEfficiently(
                context,
                userPackageModel.name,
            )

            val filesInstance = userPackage.toChildEfficiently(context, "files", FileCreatePolicy.Create(false))

            //在cache 下面创建一个文件
            val cacheInstance =
                userPackage.toChildEfficiently(context, "cache")
            val testFile =
                cacheInstance.toChildEfficiently(context, "test.txt", FileCreatePolicy.Create(true))
            testFile.getFileOutputStream().bufferedWriter().use {
                it.write("hello world")
            }
            //移动文件
            assertTrue(ScopeFileMoveOpInShell(testFile, filesInstance, context).call())
            //读取移动后的文件
            val readContent =
                filesInstance.toChildEfficiently(context, "test.txt")
                    .getFileInputStream().bufferedReader().use {
                        it.readText()
                    }
            assertEquals("hello world", readContent)
            assertEquals(false, testFile.exists())
        }
    }
}