package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermission
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter

val ftpClients = mutableMapOf<RemoteSpec, FtpInstance>()

class FtpFileInstance(uri: Uri, private val spec: RemoteSpec = RemoteSpec.parse(uri)) :
    FileInstance(uri) {
    private var ftpFile: FTPFile? = null

    private fun initCurrentFile(): FTPFile? {
        return getInstance().get(path)?.apply {
            ftpFile = this
        }
    }

    private fun getInstance(): FtpInstance {
        return ftpClients.getOrPut(spec) {
            FtpInstance(spec)
        }
    }

    val completePendingCommand
        get() = getInstance().completePendingCommand

    override suspend fun filePermissions() = reconnectIfNeed()!!.permissions()

    override suspend fun fileTime() = reconnectIfNeed()!!.fileTime()
    override suspend fun fileKind() = reconnectIfNeed()!!.fileKind1()

    private fun FTPFile.fileKind1() =
        FileKind.build(isFile, isSymbolicLink, false, fileLength(), getExtension(name).orEmpty())

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getInputStream() = getInstance().inputStream(path)!!

    override suspend fun getOutputStream() = getInstance().outputStream(path)!!

    override suspend fun listInternal(
        fileSystemPack: FileSystemPack
    ) {
        val listFiles = getInstance().listFiles(path)
        listFiles?.forEach {
            val name = it.name
            val child = childUri(name)
            val permission = it.permissions()
            val fileTime = it.fileTime()
            val kind = it.fileKind1()
            val info = FileInfo(
                name,
                child,
                fileTime,
                kind,
                permission,
            )
            if (it.isFile) {
                fileSystemPack.addFile(info)
            } else {
                fileSystemPack.addDirectory(info)
            }
        }
    }

    private fun reconnectIfNeed(): FTPFile? {
        return ftpFile ?: initCurrentFile()
    }

    override suspend fun exists(): Boolean {
        return reconnectIfNeed() != null
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): FileInstance? {
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

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }
}

class FtpInstance(private val spec: RemoteSpec) {
    private val ftp: FTPClient = FTPClient().apply {
        addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
    }

    @Throws(IOException::class)
    fun open(): Boolean {
        connect()
        return ftp.login(spec.user, spec.password)
    }

    private fun connect() {
        ftp.connect(spec.server, spec.port)
        val reply = ftp.replyCode
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect()
            throw IOException("Exception in connecting to FTP Server")
        }
    }

    fun get(path: String?): FTPFile? {
        return client { mlistFile(path) }
    }

    fun listFiles(path: String?): Array<out FTPFile>? {
        return client { listFiles(path) }
    }

    private fun <T : Any> client(block: FTPClient.() -> T): T {
        if (!ftp.isConnected || !ftp.isAvailable) {
            if (!open()) {
                throw Exception("login failed")
            }
        }
        return ftp.block()
    }

    fun inputStream(path: String): InputStream? {
        return ftp.retrieveFileStream(path)
    }

    fun outputStream(path: String): OutputStream? {
        return ftp.storeFileStream(path)
    }

    val completePendingCommand: Boolean
        get() {
            return ftp.completePendingCommand()
        }
}

fun FTPFile.permissions() = FilePermissions(
    filePermission(FTPFile.USER_ACCESS),
    filePermission(FTPFile.GROUP_ACCESS),
    filePermission(FTPFile.WORLD_ACCESS)
)

fun FTPFile.filePermission(access: Int) = FilePermission(
    hasPermission(access, FTPFile.READ_PERMISSION),
    hasPermission(access, FTPFile.WRITE_PERMISSION),
    hasPermission(access, FTPFile.EXECUTE_PERMISSION),
)

fun FTPFile.fileLength(): Long {
    return size
}
