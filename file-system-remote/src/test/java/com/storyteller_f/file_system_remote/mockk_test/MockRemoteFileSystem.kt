package com.storyteller_f.file_system_remote.mockk_test

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msdtyp.FileTime
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileAccessInformation
import com.hierynomus.msfscc.fileinformation.FileAllInformation
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.msfscc.fileinformation.FileStandardInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.protocol.commons.EnumWithValue.EnumUtils
import com.hierynomus.smbj.share.File
import com.storyteller_f.file_system.buildPath
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system_remote.FtpsInstance
import com.storyteller_f.file_system_remote.RemoteAccessType
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.SFtpInstance
import com.storyteller_f.file_system_remote.ShareSpec
import com.storyteller_f.file_system_remote.SmbInstance
import com.storyteller_f.file_system_remote.WebDavInstance
import com.storyteller_f.file_system_remote.ftpsClients
import com.storyteller_f.file_system_remote.sftpChannels
import com.storyteller_f.file_system_remote.smbSessions
import com.storyteller_f.file_system_remote.webdavInstances
import com.thegrizzlylabs.sardineandroid.DavAce
import com.thegrizzlylabs.sardineandroid.DavAcl
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.model.Ace
import com.thegrizzlylabs.sardineandroid.model.Grant
import com.thegrizzlylabs.sardineandroid.model.Principal
import com.thegrizzlylabs.sardineandroid.model.Privilege
import com.thegrizzlylabs.sardineandroid.model.Read
import com.thegrizzlylabs.sardineandroid.model.Write
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteFile
import net.schmizz.sshj.sftp.RemoteResourceInfo
import net.schmizz.sshj.xfer.FilePermission
import org.apache.commons.net.ftp.FTPFile
import org.junit.Assert
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.Calendar
import java.util.Date
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.fileSize
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isHidden
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.name
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.io.path.walk

class MockRemoteFileSystemRule(
    private val spec: RemoteSpec? = null,
    private val share: ShareSpec? = null
) : TestWatcher() {
    override fun starting(description: Description?) {
        super.starting(description)
        MockRemoteFileSystem.setup((spec?.type ?: share?.type)!!)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        MockRemoteFileSystem.close()
    }
}

object MockRemoteFileSystem {
    private var fs: FileSystem? = null

    val smbSpec = ShareSpec("localhost", 0, "test", "test", RemoteAccessType.SMB, "test1")

    val sftpSpec = RemoteSpec("localhost", 0, "test", "test", RemoteAccessType.SFTP)

    val ftpsSpec = RemoteSpec("localhost", 0, "test", "test", RemoteAccessType.FTPS)

    val webDavSpec =
        RemoteSpec("localhost", 0, "test", "test", type = RemoteAccessType.WEB_DAV)

    fun setup(type: String) {
        val newFileSystem = Jimfs.newFileSystem(Configuration.unix())
        fs = newFileSystem
        newFileSystem.getPath("/test1").apply {
            createDirectory()
        }
        val helloFile = newFileSystem.getPath("/test1/hello.txt").apply {
            createFile()
        }
        helloFile.outputStream().bufferedWriter().use {
            it.write("world smb")
        }

        when (type) {
            RemoteAccessType.SMB -> bindSmbSession()
            RemoteAccessType.SFTP -> bindSFtpSession()
            RemoteAccessType.FTPS -> bindFtpsSession()
            RemoteAccessType.WEB_DAV -> bindWebDavSession(webDavSpec)
        }
    }

    suspend fun commonTest(fileInstance: FileInstance) {
        val list = fileInstance.list()
        Assert.assertEquals(1, list.count)
        val childInstance = fileInstance.toChild("hello.txt", FileCreatePolicy.NotCreate)!!
        Assert.assertTrue(childInstance.fileKind().isFile)

        val helloTxtLastModified =
            fs!!.getPath("/test1/hello.txt").readAttributes<BasicFileAttributes>()
                .lastModifiedTime()
                .toMillis()
        Assert.assertEquals(
            helloTxtLastModified,
            childInstance.fileTime().lastModified
        )
        Assert.assertEquals(helloTxtLastModified, childInstance.getFileInfo().time.lastModified)
        Assert.assertTrue(childInstance.filePermissions().userPermission.readable)
        val text = childInstance.getInputStream().bufferedReader().use {
            it.readText()
        }
        Assert.assertEquals("world smb", text)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun bindWebDavSession(webDavSpec: RemoteSpec) {
        val origin = WebDavInstance(webDavSpec)
        spyk(origin) {
            every {
                list(any())
            } answers {
                val realPath = firstArg<String>().substring(origin.baseUrl.length)
                fs!!.getPath(realPath).walk().map {
                    mockDavResources(it)
                }.toMutableList()
            }
            every {
                getInputStream(any())
            } answers {
                val realPath = firstArg<String>().substring(origin.baseUrl.length)
                fs!!.getPath(realPath).inputStream()
            }
            every {
                acl(any())
            } answers {
                val realPath = firstArg<String>().substring(origin.baseUrl.length)
                mockAcl(realPath)
            }
            every {
                resources(any())
            } answers {
                val realPath = firstArg<String>().substring(origin.baseUrl.length)
                mockDavResources(fs!!.getPath(realPath))
            }
        }.apply {
            webdavInstances[MockRemoteFileSystem.webDavSpec] = this
        }
    }

    private fun mockAcl(path: String): DavAcl {
        val p = fs!!.getPath(path)
        return mockk<DavAcl> {
            every {
                aces
            } returns listOf(DavAce(Ace().apply {
                principal = Principal()
                grant = Grant().apply {
                    privilege = listOf(Privilege().apply {
                        if (p.isReadable()) {
                            content.add(Read())
                        }
                        if (p.isWritable()) {
                            content.add(Write())
                        }
                    })
                }
            }))
        }
    }

    private fun mockDavResources(it: Path): DavResource {
        val basicFileAttributes = it.readAttributes<BasicFileAttributes>()
        return mockk<DavResource> {
            every {
                name
            } returns it.name
            every {
                isDirectory
            } returns it.isDirectory()
            every {
                modified
            } returns Date(basicFileAttributes.lastModifiedTime().toMillis())
            every {
                creation
            } returns Date(basicFileAttributes.creationTime().toMillis())
            every {
                path
            } returns it.pathString
            every {
                contentLength
            } returns it.fileSize()
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun bindFtpsSession() {
        mockk<FtpsInstance> {
            every {
                listFiles(any())
            } answers {
                fs!!.getPath(firstArg()).walk().map {
                    mockFtpsFile(it)
                }.toList().toTypedArray<FTPFile>()
            }
            every {
                inputStream(any())
            } answers {
                fs!!.getPath(firstArg()).inputStream()
            }
            every {
                outputStream(any())
            } answers {
                fs!!.getPath(firstArg()).outputStream()
            }
            every {
                getFile(any())
            } answers {
                mockFtpsFile(fs!!.getPath(firstArg()))
            }
        }.apply {
            ftpsClients[ftpsSpec] = this
        }
    }

    private fun mockFtpsFile(p: Path): FTPFile {
        val basicFileAttributes = p.readAttributes<BasicFileAttributes>()
        return mockk<FTPFile> {
            every {
                hasPermission(any(), any())
            } answers {
                val access = firstArg<Int>()
                val permission = secondArg<Int>()
                if (access == FTPFile.USER_ACCESS) {
                    when (permission) {
                        FTPFile.READ_PERMISSION -> p.isReadable()
                        FTPFile.WRITE_PERMISSION -> p.isWritable()
                        FTPFile.EXECUTE_PERMISSION -> p.isExecutable()
                        else -> throw IllegalArgumentException()
                    }
                } else {
                    false
                }
            }
            every {
                isSymbolicLink
            } returns basicFileAttributes.isSymbolicLink
            every {
                isFile
            } returns p.isRegularFile()
            every {
                isDirectory
            } returns p.isDirectory()
            every {
                name
            } returns p.name
            every {
                timestamp
            } returns Calendar.getInstance().apply {
                timeInMillis = basicFileAttributes.lastModifiedTime().toMillis()
            }
            every {
                size
            } returns p.fileSize()
        }
    }

    fun close() {
        smbSessions.clear()
        sftpChannels.clear()
        fs!!.close()
    }

    private fun bindSFtpSession() {
        mockk<SFtpInstance> {
            every {
                ls(any())
            } answers {
                mockSFtpResponse(firstArg())
            }
            every {
                open(any())
            } answers {
                mockSFtpRemoteFile(firstArg())
            }
        }.apply {
            sftpChannels[sftpSpec] = this
        }
    }

    private fun mockSFtpRemoteFile(path: String): RemoteFile {
        val pathObject = fs!!.getPath(path)
        val basicFileAttributes = pathObject.readAttributes<BasicFileAttributes>()
        return mockk {
            every {
                fetchAttributes()
            } answers {
                net.schmizz.sshj.sftp.FileAttributes.Builder()
                    .withAtimeMtime(
                        basicFileAttributes.lastAccessTime().toMillis(),
                        basicFileAttributes.lastModifiedTime().toMillis()
                    )
                    .withPermissions(buildPermission(pathObject))
                    .withType(
                        if (pathObject.isRegularFile()) {
                            FileMode.Type.REGULAR
                        } else {
                            FileMode.Type.DIRECTORY
                        }
                    ).build()
            }
            every {
                read(any(), any(), any(), any())
            } answers {
                pathObject.inputStream().use {
                    it.skip(firstArg())
                    it.read(secondArg(), thirdArg(), lastArg())
                }
            }
            every {
                close()
            } just Runs
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun mockSFtpResponse(path: String) =
        fs!!.getPath(path).walk().map { pathObject ->
            val basicFileAttributes = pathObject.readAttributes<BasicFileAttributes>()
            mockk<RemoteResourceInfo> {
                every {
                    attributes
                } answers {
                    mockSFtpAttributes(pathObject, basicFileAttributes)
                }
                every {
                    isDirectory
                } returns pathObject.isDirectory()
                every {
                    isRegularFile
                } returns pathObject.isRegularFile()
                every {
                    name
                } returns pathObject.name
            }
        }.toList()

    private fun mockSFtpAttributes(
        pathObject: Path,
        basicFileAttributes: BasicFileAttributes,
    ): net.schmizz.sshj.sftp.FileAttributes = mockk {
        every {
            mode
        } returns FileMode(com.storyteller_f.slim_ktx.buildMask {
            if (Files.isSymbolicLink(pathObject)) FileMode.Type.SYMLINK
        })
        every {
            atime
        } returns basicFileAttributes.lastAccessTime().toMillis()
        every {
            mtime
        } returns basicFileAttributes.lastModifiedTime().toMillis()
        every {
            permissions
        } returns buildPermission(pathObject)
        every {
            size
        } returns pathObject.fileSize()
    }

    private fun buildPermission(pathObject: Path?) = buildSet {
        if (Files.isReadable(pathObject)) add(FilePermission.USR_R)
        if (Files.isWritable(pathObject)) add(FilePermission.USR_W)
        if (Files.isExecutable(pathObject)) add(FilePermission.USR_X)
    }

    private fun bindSmbSession() {
        mockk<SmbInstance> {
            every {
                list(any())
            } answers {
                mockSmbListResponse(smbSpec, firstArg())
            }
            every {
                information(any(String::class))
            } answers {
                mockSmbInformation(firstArg<String>())
            }
            every {
                open(any(), any(), any(), any(), SMB2CreateDisposition.FILE_OPEN, any())
            } answers {
                mockSmbInputStream(firstArg())
            }
        }.apply {
            smbSessions[smbSpec] = this
        }
    }

    private fun mockSmbInformation(path: String): FileAllInformation {
        val buildPath = buildPath(smbSpec.share, path)

        return mockk {
            every {
                standardInformation
            } answers {
                fileStandardInformation(buildPath)
            }
            every {
                basicInformation
            } answers {
                mockk {
                    every {
                        standardInformation
                    } answers {
                        fileStandardInformation(buildPath)
                    }
                    every {
                        fileAttributes
                    } answers {
                        mockSmbFileAttributes(buildPath)
                    }
                    every {
                        changeTime
                    } answers {
                        val p = fs!!.getPath(buildPath)
                        FileTime.ofEpochMillis(
                            p.readAttributes<BasicFileAttributes>().lastModifiedTime().toMillis()
                        )
                    }
                    every {
                        creationTime
                    } answers {
                        val p = fs!!.getPath(buildPath)
                        FileTime.ofEpochMillis(
                            p.readAttributes<BasicFileAttributes>().creationTime().toMillis()
                        )
                    }
                    every {
                        lastAccessTime
                    } answers {
                        val p = fs!!.getPath(buildPath)
                        FileTime.ofEpochMillis(
                            p.readAttributes<BasicFileAttributes>().lastAccessTime().toMillis()
                        )
                    }
                    every {
                        accessInformation
                    } answers {
                        mockAccessInformation(buildPath)
                    }
                }
            }
        }
    }

    private fun mockAccessInformation(buildPath: String): FileAccessInformation {
        val p = fs!!.getPath(buildPath)
        return mockk {
            every {
                accessFlags
            } answers {
                EnumUtils.toLong(buildSet<AccessMask> {
                    if (p.isReadable()) {
                        add(AccessMask.GENERIC_READ)
                    }
                    if (p.isWritable()) {
                        add(AccessMask.GENERIC_WRITE)
                    }
                    if (p.isExecutable()) {
                        add(AccessMask.GENERIC_EXECUTE)
                    }
                }).toInt()
            }
        }
    }

    private fun mockSmbFileAttributes(buildPath: String): Long {
        val p = fs!!.getPath(buildPath)
        return EnumUtils.toLong(buildSet<FileAttributes> {
            if (p.isDirectory()) {
                add(FileAttributes.FILE_ATTRIBUTE_DIRECTORY)
            }
            if (p.isHidden()) {
                add(FileAttributes.FILE_ATTRIBUTE_HIDDEN)
            }
        })
    }

    private fun fileStandardInformation(buildPath: String): FileStandardInformation =
        mockk {
            every {
                isDirectory
            } returns fs!!.getPath(buildPath).isDirectory()
            every {
                allocationSize
            } returns fs!!.getPath(buildPath).fileSize()
        }

    private fun mockSmbInputStream(relativePath: String): File =
        mockk {
            every {
                inputStream
            } returns fs!!.getPath(
                buildPath(smbSpec.share, relativePath)
            ).inputStream()
        }

    @OptIn(ExperimentalPathApi::class)
    private fun mockSmbListResponse(
        shareSpec: ShareSpec,
        relativePath: String,
    ): List<FileIdBothDirectoryInformation> {
        val buildPath = buildPath(shareSpec.share, relativePath)
        return fs!!.getPath(buildPath).walk().filter {
            it.name.isNotEmpty()
        }.map { pathObject ->
            mockSmbInformation(pathObject)
        }.toList()
    }

    private fun mockSmbInformation(pathObject: Path) =
        mockk<FileIdBothDirectoryInformation> {
            every {
                fileName
            } returns pathObject.name
            every {
                fileAttributes
            } answers {
                mockSmbFileAttributes(pathObject.pathString)
            }
            val fileAttributes = pathObject.readAttributes<BasicFileAttributes>()
            every {
                changeTime
            } returns FileTime(fileAttributes.lastModifiedTime().toMillis())
            every {
                creationTime
            } returns FileTime(fileAttributes.creationTime().toMillis())
            every {
                lastAccessTime
            } returns FileTime(fileAttributes.lastAccessTime().toMillis())
            every {
                allocationSize
            } returns if (pathObject.isRegularFile()) pathObject.fileSize() else 0
        }
}
