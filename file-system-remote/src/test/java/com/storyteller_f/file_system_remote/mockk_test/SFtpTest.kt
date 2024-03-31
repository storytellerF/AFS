package com.storyteller_f.file_system_remote.mockk_test

import com.storyteller_f.file_system.getFileInstance
import io.mockk.junit4.MockKRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SFtpTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val commonRelu = MockRemoteFileSystemRule(MockRemoteFileSystem.sftpSpec)

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        val test1Spec = MockRemoteFileSystem.sftpSpec
        val uri = test1Spec.toUri().buildUpon().appendPath("test1").build()
        runBlocking {
            val sFtpFileInstance = getFileInstance(context, uri)!!
            MockRemoteFileSystem.commonTest(sFtpFileInstance)
        }
    }
}
