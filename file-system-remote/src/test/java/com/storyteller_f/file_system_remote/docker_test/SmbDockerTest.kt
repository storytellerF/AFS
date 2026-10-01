package com.storyteller_f.file_system_remote.docker_test

import com.storyteller_f.file_system.getFileInstance
import com.storyteller_f.file_system_remote.RemoteSchemes
import com.storyteller_f.file_system_remote.ShareSpec
import com.storyteller_f.file_system_remote.checkSmbConnection
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

@RunWith(RobolectricTestRunner::class)
class SmbDockerTest {

    private lateinit var container: GenericContainer<*>

    @Before
    fun setup() {
        System.setProperty("api.version", "1.44")
        container = GenericContainer(DockerImageName.parse("dockurr/samba:latest"))
            .withExposedPorts(445).apply {
                addEnv("USER", "myuser")
                addEnv("PASS", "mypassword")
            }
        container.start()
    }

    @After
    fun teardown() {
        container.stop()
    }

    @Test
    fun test() {

        val host = container.host
        val port = container.getMappedPort(445)
        val remoteSpec =
            ShareSpec(host, port, "myuser", "mypassword", RemoteSchemes.SMB, "Data")
        remoteSpec.checkSmbConnection()
        val uri = remoteSpec.toUri()
        runBlocking {
            getFileInstance(uri)!!.exists()
        }
    }
}
