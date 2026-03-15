package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system_remote.RemoteSchemes
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.checkFtpsConnection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FtpsDockerTest {

    private lateinit var container: GenericContainer<*>

    @Before
    fun setup() {
        System.setProperty("api.version", "1.44")
        container = GenericContainer(DockerImageName.parse("pcavezzan/ftpsdev:latest"))
            .withExposedPorts(21).apply {
                addEnv("FTP_USER", "myuser")
                addEnv("FTP_PWD", "mypassword")
            }
        container.start()
    }

    @After
    fun teardown() {
        container.stop()
    }

    @Test
    fun test() {
        val context = RuntimeEnvironment.getApplication()

        val host = container.host
        val port = container.getMappedPort(21)
        val remoteSpec =
            RemoteSpec(host, port, "myuser", "mypassword", RemoteSchemes.FTP_ES)
        remoteSpec.checkFtpsConnection()
        val uri = remoteSpec.toUri()
        runBlocking {
            getFileInstance(context, uri)!!.exists()
        }
    }
}
