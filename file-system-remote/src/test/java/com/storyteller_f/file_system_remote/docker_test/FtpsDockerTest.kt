package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system_remote.RemoteSchemes
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.checkFtpsConnection
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FtpsDockerTest {

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        Assume.assumeTrue("未安装 pcavezzan/ftpsdev", isDockerContainerRunning("ftps"))
        val remoteSpec =
            RemoteSpec("localhost", 2121, "myuser", "mypassword", RemoteSchemes.FTP_ES)
        remoteSpec.checkFtpsConnection()
        val uri = remoteSpec.toUri()
        runBlocking {
            getFileInstance(context, uri)!!.exists()
        }
    }
}
