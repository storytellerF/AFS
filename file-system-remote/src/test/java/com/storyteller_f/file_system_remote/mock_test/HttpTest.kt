package com.storyteller_f.file_system_remote.mock_test

import android.net.Uri
import com.storyteller_f.file_system.size
import com.storyteller_f.file_system_remote.HttpFileInstance
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class HttpTest {
    companion object {
        private lateinit var server: MockWebServer

        @JvmStatic
        @BeforeClass
        fun init() {
            server = MockWebServer().apply {
                enqueue(MockResponse().apply {
                    addHeader("content-type", "text/plain")
                    addHeader("Content-Disposition", "attachment; filename=\"test.text\"")
                    setBody("hello world")
                })
                start()
            }
        }

        @JvmStatic
        @AfterClass
        fun close() {
            server.close()
        }
    }

    @Test
    fun test() {
        val appContext = RuntimeEnvironment.getApplication()

        val uri = Uri.parse("http://localhost:${server.port}")
        val httpFileInstance = HttpFileInstance(appContext, uri)
        runBlocking {
            httpFileInstance.getInputStream().bufferedReader().use {
                val readText = it.readText()
                assertEquals("hello world", readText)
            }
            assertTrue(httpFileInstance.fileKind().isFile)
            assertFalse(httpFileInstance.fileKind().isDirectory)
            assertEquals(11, httpFileInstance.size())
            assertFalse(httpFileInstance.createFile())
            assertFalse(httpFileInstance.createDirectory())
        }
    }
}
