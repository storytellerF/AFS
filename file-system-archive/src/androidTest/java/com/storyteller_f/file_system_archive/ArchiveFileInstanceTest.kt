package com.storyteller_f.file_system_archive

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system.ensureFile
import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.toChildEfficiently
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class ArchiveFileInstanceTest {

    @Test
    fun testArchiveFileInstance() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            val file =
                appContext.buildZip("test.zip", listOf(Node("hello.txt", emptyList(), "hello")))

            val archiveFileInstance = getFileInstance(
                appContext,
                ArchiveFileInstanceFactory.buildNestedFile(Uri.fromFile(file), null)!!
            )!!
            val list = archiveFileInstance.list()
            assertEquals("hello.txt", list.files.first().name)

            val helloTxtFileInstance =
                archiveFileInstance.toChild("hello.txt", FileCreatePolicy.NotCreate)!!

            assertEquals(true, helloTxtFileInstance.fileKind().isFile)
            val content = helloTxtFileInstance.getInputStream().bufferedReader().use {
                it.readText()
            }
            assertEquals("hello", content)
        }
    }

    @Test
    fun testLocalArchiveFileInstanceToChildEfficiently() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            val zipFile =
                appContext.buildZip("test.zip", listOf(Node("hello.txt", emptyList(), "hello")))

            val fileInstance = getFileInstance(appContext, Uri.fromFile(zipFile))!!
            val instance = fileInstance.toChildEfficiently(appContext, "hello.txt")
            assertEquals("hello.txt", instance.name)
        }

    }

    @Test
    fun testNestedArchiveFileInstance() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            val zipFile =
                appContext.buildZip("test.zip", listOf(Node("hello.txt", emptyList(), "hello")))

            val parentZip = File(appContext.filesDir, "parent.zip").ensureFile()!!
            ZipOutputStream(parentZip.outputStream()).use {
                val zipEntry = ZipEntry("test.zip")
                it.putNextEntry(zipEntry)
                zipFile.inputStream().use { input ->
                    writeStream(input, it)
                }
            }
            val parentFileInstance = getFileInstance(appContext, Uri.fromFile(parentZip))!!
            val instance = parentFileInstance.toChildEfficiently(appContext, "test.zip")
            assertEquals("test.zip", instance.name)
            val textFileInstance = instance.toChildEfficiently(appContext, "hello.txt")
            assertEquals("hello.txt", textFileInstance.name)
        }

    }

    @Test
    fun testMultiEntry() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            val zipFile = appContext.buildZip(
                "test.zip",
                listOf(
                    Node("hello.txt", emptyList(), "hello"),
                    Node(
                        "hello", listOf(
                            Node("text.html", emptyList(), ""),
                            Node("world", emptyList())
                        )
                    )
                )
            )

            val zipUri = Uri.fromFile(zipFile)
            val archiveUri = ArchiveFileInstanceFactory.buildNestedFile(zipUri, null)!!
            val fileInstance = getFileInstance(appContext, archiveUri)!!
            val pack = fileInstance.list()
            assertEquals(2, pack.count)
            assertEquals("/hello.txt", pack.files.first().fullPath)
            assertEquals("/hello", pack.directories.first().fullPath)
            //test toChild
            val child = fileInstance.toChild("hello", FileCreatePolicy.NotCreate)!!
            val childPack = child.list()
            assertEquals(2, childPack.count)
            assertEquals("/hello/text.html", childPack.files.first().fullPath)
            assertEquals("/hello/world", childPack.directories.first().fullPath)
        }
    }

    @Test
    fun testParent() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            val zipFile = appContext.buildZip(
                "test.zip",
                listOf(
                    Node("hello.txt", emptyList(), "hello"),
                    Node(
                        "hello", listOf(
                            Node("text.html", emptyList(), ""),
                            Node("world", emptyList())
                        )
                    )
                )
            )

            val zipUri = Uri.fromFile(zipFile)
            val archiveUri = ArchiveFileInstanceFactory.buildNestedFile(zipUri, "/hello/world")!!
            val fileInstance = getFileInstance(appContext, archiveUri)!!
            val parentInstance = fileInstance.toParent()
            assertEquals("hello", parentInstance.name)
        }
    }

    @Test
    fun testExists() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        runBlocking {
            val zipFile = appContext.buildZip(
                "test.zip",
                listOf(
                    Node("hello.txt", emptyList(), "hello"),
                )
            )

            val zipUri = Uri.fromFile(zipFile)
            val archiveUri = ArchiveFileInstanceFactory.buildNestedFile(zipUri, "/hello")!!
            val fileInstance = getFileInstance(appContext, archiveUri)!!
            assertEquals(false, fileInstance.exists())
        }
    }
}

suspend fun writeStream(inputStream: InputStream, outputStream: OutputStream) {
    val array = ByteArray(DEFAULT_BUFFER_SIZE)
    withContext(Dispatchers.IO) {
        while (true) {
            val count = inputStream.read(array)
            if (count >= 0) {
                outputStream.write(array, 0, count)
            } else {
                break
            }
        }
    }
}