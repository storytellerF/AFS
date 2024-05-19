package com.storyteller_f.file_system_remote.instance

import android.net.Uri
import com.storyteller_f.file_system.getExtension
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import com.storyteller_f.file_system.model.FileSystemPack
import com.storyteller_f.file_system_remote.RemoteSpec
import com.thegrizzlylabs.sardineandroid.DavAcl
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.Date

val webdavInstances = mutableMapOf<RemoteSpec, WebDavInstance>()

class WebDavFileInstance(uri: Uri, private val spec: RemoteSpec = RemoteSpec.parse(uri)) :
    FileInstance(uri) {

    private var resources: DavResource? = null
    private var acl: DavAcl? = null

    private fun reconnectResourcesIfNeed(): DavResource {
        val r = resources
        if (r == null) {
            val resources1 = getWebDavInstance().run {
                resources(buildRelativePath(path))
            }
            resources = resources1
            return resources1
        }
        return r
    }

    private fun reconnectAclIfNeed(): DavAcl {
        val r = acl
        if (r == null) {
            val resources1 = getWebDavInstance().run {
                acl(buildRelativePath(path))!!
            }
            acl = resources1
            return resources1
        }
        return r
    }

    override suspend fun filePermissions() = reconnectAclIfNeed().filePermissions()

    private fun DavAcl.filePermissions(): FilePermissions {
        val granted = aces.flatMap { ace ->
            ace.granted
        }
        return FilePermissions.permissions(
            granted.contains("read"),
            granted.contains("write"),
            false
        )
    }

    override suspend fun fileTime() = reconnectResourcesIfNeed().let {
        FileTime(it.modified.time, null, it.creation.time)
    }

    override suspend fun fileKind(): FileKind {
        return reconnectResourcesIfNeed().fileKind1()
    }

    private fun DavResource.fileKind1() = FileKind.build(
        !isDirectory,
        false,
        isHidden = false,
        size = fileLength(),
        getExtension(name).orEmpty()
    )

    private fun getWebDavInstance() = webdavInstances.getOrPut(spec) {
        WebDavInstance(spec)
    }

    override suspend fun getInputStream(): InputStream {
        return getWebDavInstance().run {
            getInputStream(buildRelativePath(path))!!
        }
    }

    override suspend fun getOutputStream(): OutputStream {
        return super.getOutputStream()
    }

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(
        fileSystemPack: FileSystemPack,
    ) {
        val webDavInstance = getWebDavInstance()
        webDavInstance.list(webDavInstance.buildRelativePath(path)).filter {
            it.path != "$path/"
        }.forEach {
            val fileName = it.name
            val child = childUri(fileName)
            val filePermissions =
                webDavInstance.acl(webDavInstance.buildAbsolutePath(it.path))?.filePermissions()!!
            val modified: Date? = it.modified
            val creation: Date? = it.creation
            val fileTime = FileTime(modified?.time, created = creation?.time)
            val kind = it.fileKind1()
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

    override suspend fun exists() = getWebDavInstance().run {
        exists(buildRelativePath(path))
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        getWebDavInstance().run {
            delete(buildRelativePath(path))
        }
        return true
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
        getWebDavInstance().run {
            createDirectory(buildRelativePath(path))
        }
        return true
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        val new = childUri(name)
        return WebDavFileInstance(new, spec)
    }
}

class WebDavInstance(private val spec: RemoteSpec) {
    internal val baseUrl by lazy {
        val server = if (spec.server.contains("/")) {
            spec.server.replaceFirst("/", ":${spec.port}/")
        } else {
            "${spec.server}:${spec.port}"
        }
        "http://$server"
    }
    private val prefixPath by lazy {
        if (spec.server.contains("/")) {
            spec.server.substringAfter("/")
        } else {
            ""
        }
    }
    private val instance = OkHttpSardine().apply {
        setCredentials(spec.user, spec.password)
    }

    fun list(path: String): MutableList<DavResource> = instance.list(path)

    fun resources(path: String): DavResource {
        return instance.getResources(path).filterNotNull().first()
    }

    fun getInputStream(path: String): InputStream? {
        return instance.get(path)
    }

    fun exists(path: String): Boolean {
        return instance.exists(path)
    }

    fun delete(path: String) {
        instance.delete(path)
    }

    fun createDirectory(path: String) {
        instance.createDirectory(path)
    }

    fun acl(path: String): DavAcl? {
        return instance.getAcl(path)
    }

    internal fun buildAbsolutePath(path: String) = baseUrl + path.substring(prefixPath.length + 1)

    internal fun buildRelativePath(path: String) = baseUrl + path
}

fun DavResource.fileLength(): Long {
    return contentLength ?: 0
}
