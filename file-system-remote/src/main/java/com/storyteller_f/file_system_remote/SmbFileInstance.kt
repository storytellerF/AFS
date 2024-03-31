package com.storyteller_f.file_system_remote

import android.net.Uri
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.protocol.commons.EnumWithValue.EnumUtils
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

val smbSessions = mutableMapOf<ShareSpec, SmbInstance>()

class SmbFileInstance(uri: Uri, private val shareSpec: ShareSpec = ShareSpec.parse(uri)) :
    FileInstance(uri) {
    private var information: FileAllInformation? = null
    override val path: String
        get() = super.path.substring(shareSpec.share.length + 1).ifEmpty { "/" }

    override suspend fun filePermissions(): FilePermissions {
        val second = reconnectIfNeed()
        val accessFlags = second.accessInformation.accessFlags.toLong()
        return FilePermissions.permissions(
            EnumUtils.isSet(accessFlags, AccessMask.GENERIC_READ),
            EnumUtils.isSet(accessFlags, AccessMask.GENERIC_READ),
            EnumUtils.isSet(accessFlags, AccessMask.GENERIC_READ)
        )
    }

    override suspend fun fileTime() = reconnectIfNeed().fileTime()
    override suspend fun fileKind() = reconnectIfNeed().let { allInformation ->
        FileKind.build(
            !allInformation.standardInformation.isDirectory,
            isSymbolicLink = false,
            isHidden = EnumUtils.isSet(
                allInformation.basicInformation.fileAttributes,
                FileAttributes.FILE_ATTRIBUTE_HIDDEN
            ),
            allInformation.fileLength(),
            extension
        )
    }

    private fun initInformation(): FileAllInformation {
        val fileInformation = getDiskShare().information(path)
        information = fileInformation
        return fileInformation
    }

    private fun getDiskShare(): SmbInstance {
        return smbSessions.getOrPut(shareSpec) {
            SmbInstance(shareSpec)
        }
    }

    private fun reconnectIfNeed(): FileAllInformation {
        var information = information
        if (information == null) {
            information = initInformation()
        }
        return information
    }

    override suspend fun getInputStream(): InputStream = getDiskShare().open(
        path,
        emptySet(),
        emptySet(),
        emptySet(),
        SMB2CreateDisposition.FILE_OPEN,
        emptySet()
    )!!.inputStream

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileSystemPack: FileSystemPack
    ) {
        getDiskShare().list(path).filter {
            it.fileName != "." && it.fileName != ".."
        }.forEach {
            val fileName = it.fileName
            val child = childUri(fileName)
            val isDirectory =
                EnumUtils.isSet(it.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY)
            val fileTime = it.fileTime()
            val filePermissions = FilePermissions.USER_READABLE
            val kind = FileKind.build(
                isFile = !isDirectory,
                isSymbolicLink = false,
                isHidden = false,
                it.allocationSize,
                getExtension(fileName).orEmpty()
            )
            val info = FileInfo(
                fileName,
                child,
                fileTime,
                kind,
                filePermissions,
            )
            if (isDirectory) {
                fileSystemPack.addDirectory(info)
            } else {
                fileSystemPack.addFile(info)
            }
        }
    }

    override suspend fun exists() = runCatching {
        reconnectIfNeed()
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
        SmbFileInstance(childUri(name), shareSpec)
}

class SmbInstance(private val shareSpec: ShareSpec) {
    private val smbClient by lazy {
        SMBClient()
    }

    private fun <T> client(block: DiskShare.() -> T): T {
        return requireDiskShare(shareSpec).use(block)
    }

    private fun requireDiskShare(shareSpec: ShareSpec): DiskShare {
        val connect = smbClient.connect(shareSpec.server, shareSpec.port)
        val authenticationContext =
            AuthenticationContext(shareSpec.user, shareSpec.password.toCharArray(), null)
        val session = connect.authenticate(authenticationContext)
        return session.connectShare(shareSpec.share) as DiskShare
    }

    fun information(path: String): FileAllInformation {
        return client {
            getFileInformation(path)
        }
    }

    fun open(
        path: String,
        accessMasks: Set<AccessMask>,
        attributes: Set<FileAttributes>,
        shareAccesses: Set<SMB2ShareAccess>,
        disposition: SMB2CreateDisposition,
        optionsSet: Set<SMB2CreateOptions>
    ): com.hierynomus.smbj.share.File? {
        return client {
            openFile(path, accessMasks, attributes, shareAccesses, disposition, optionsSet)
        }
    }

    fun list(path: String): List<FileIdBothDirectoryInformation> {
        return client {
            list(path)
        }
    }
}

private fun FileIdBothDirectoryInformation.fileTime() =
    FileTime(
        changeTime.toEpochMillis(),
        lastAccessTime.toEpochMillis(),
        creationTime.toEpochMillis()
    )

private fun FileAllInformation.fileTime(): FileTime {
    val basicInformation = basicInformation
    return FileTime(
        basicInformation.changeTime.toEpochMillis(),
        basicInformation.lastAccessTime.toEpochMillis(),
        basicInformation.creationTime.toEpochMillis()
    )
}

fun FileAllInformation.fileLength(): Long {
    return standardInformation.allocationSize
}
