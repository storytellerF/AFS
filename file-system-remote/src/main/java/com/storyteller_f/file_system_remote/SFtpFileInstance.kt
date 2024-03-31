package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import com.storyteller_f.slim_ktx.bit
import net.schmizz.sshj.AndroidConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileAttributes
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.FilePermission
import java.io.FileInputStream
import java.io.FileOutputStream

val sftpChannels = mutableMapOf<RemoteSpec, SFtpInstance>()

class SFtpFileInstance(uri: Uri, private val spec: RemoteSpec = RemoteSpec.parse(uri)) :
    FileInstance(uri) {
    private var fileAttributes: FileAttributes? = null

    private fun getInstance() = sftpChannels.getOrPut(spec) {
        SFtpInstance(spec)
    }

    private fun fetchAttributesIfNeed(): FileAttributes {
        val attributes = fileAttributes
        if (attributes == null) {
            val fetchAttributes = getInstance().open(path)!!.use {
                it.fetchAttributes()!!
            }
            fileAttributes = fetchAttributes
            return fetchAttributes
        }
        return attributes
    }

    override suspend fun filePermissions() =
        fetchAttributesIfNeed().permissions.filePermissions()

    private fun MutableSet<FilePermission>.filePermissions(): FilePermissions {
        return FilePermissions(
            com.storyteller_f.file_system.instance.FilePermission(
                contains(
                    FilePermission.USR_X
                ),
                contains(FilePermission.USR_R),
                contains(FilePermission.USR_W)
            ),
            com.storyteller_f.file_system.instance.FilePermission(
                contains(
                    FilePermission.GRP_X
                ),
                contains(FilePermission.GRP_R),
                contains(FilePermission.GRP_W)
            ),
            com.storyteller_f.file_system.instance.FilePermission(
                contains(
                    FilePermission.OTH_X
                ),
                contains(FilePermission.OTH_R),
                contains(FilePermission.OTH_W)
            )
        )
    }

    override suspend fun fileTime() = fetchAttributesIfNeed().fileTime()

    override suspend fun fileKind() = fetchAttributesIfNeed().let { fileAttributes ->
        val type = fileAttributes.mode.type
        val typeMask = type.toMask()
        FileKind.build(
            typeMask.bit(FileMode.Type.REGULAR.toMask()),
            typeMask.bit(FileMode.Type.SYMLINK.toMask()),
            false,
            fileAttributes.fileLength(),
            extension
        )
    }

    override suspend fun getInputStream() =
        getInstance().open(path)!!.RemoteFileInputStream()

    override suspend fun getOutputStream() =
        getInstance().open(path)!!.RemoteFileOutputStream()

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileSystemPack: FileSystemPack
    ) {
        getInstance().ls(path).forEach {
            val attributes = it.attributes
            val fileName = it.name
            val child = childUri(fileName)
            val isSymLink = attributes.mode.type.toMask().bit(FileMode.Type.SYMLINK.toMask())
            val fileTime = attributes.fileTime()
            val filePermissions = attributes.permissions.filePermissions()
            val kind = FileKind.build(
                it.isRegularFile,
                isSymLink,
                false,
                it.fileLength(),
                getExtension(fileName).orEmpty()
            )
            val info = FileInfo(
                fileName,
                child,
                fileTime,
                kind,
                filePermissions
            )
            if (it.isDirectory) {
                fileSystemPack.addDirectory(info)
            } else {
                fileSystemPack.addFile(info)
            }
        }
    }

    override suspend fun exists() = runCatching {
        fetchAttributesIfNeed()
    }.isSuccess

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy) =
        SFtpFileInstance(childUri(name), spec)
}

class SFtpInstance(private val spec: RemoteSpec) {

    private fun <T : Any> client(block: SFTPClient.() -> T): T {
        val sshClient = SSHClient(AndroidConfig()).apply {
            addHostKeyVerifier(PromiscuousVerifier())
            connect(spec.server, spec.port)
            authPassword(spec.user, spec.password)
        }
        return sshClient.use {
            it.newSFTPClient().use(block)
        }
    }

    fun open(path: String): RemoteFile? {
        return client {
            open(path)
        }
    }

    fun ls(path: String): List<RemoteResourceInfo> {
        return client {
            this.sftpEngine.subsystem
            ls(path)
        }
    }
}

private fun FileAttributes.fileTime() = FileTime(mtime, atime)

private fun FileAttributes.fileLength(): Long {
    return size
}

private fun RemoteResourceInfo.fileLength(): Long {
    return attributes.size
}
