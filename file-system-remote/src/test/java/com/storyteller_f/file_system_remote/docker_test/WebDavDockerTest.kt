package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system_remote.RemoteAccessType
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.checkWebDavConnection
import kotlinx.coroutines.runBlocking
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WebDavDockerTest {

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        Assume.assumeTrue("未安装 ionelmc/webdav", isDockerContainerRunning("webdav"))
        val remoteSpec =
            RemoteSpec("localhost", 7000, "myuser", "mypassword", RemoteAccessType.WEB_DAV)
        remoteSpec.checkWebDavConnection()
        val uri = remoteSpec.toUri()
        runBlocking {
            getFileInstance(context, uri)!!.exists()
        }
    }
}

fun isDockerContainerRunning(dockerServerName: String): Boolean {
    return try {
        val process =
            Runtime.getRuntime()
                .exec("docker ps --format \"{{.Names}}\" --filter name=$dockerServerName")
        process.inputStream.bufferedReader().lineSequence().contains(dockerServerName)
    } catch (_: Exception) {
        false
    }
}
