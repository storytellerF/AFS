package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system_remote.RemoteSchemes
import com.storyteller_f.file_system_remote.ShareSpec
import com.storyteller_f.file_system_remote.checkSmbConnection
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SmbDockerTest {

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        Assume.assumeTrue("未安装 dockurr/samba", isDockerContainerRunning("samba"))
        val remoteSpec =
            ShareSpec("localhost", 1445, "myuser", "mypassword", RemoteSchemes.SMB, "Data")
        remoteSpec.checkSmbConnection()
        val uri = remoteSpec.toUri()
        runBlocking {
            getFileInstance(context, uri)!!.exists()
        }
    }
}
