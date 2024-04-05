package com.storyteller_f.file_system

import android.content.Context
import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import java.util.ServiceLoader

interface FileSystemPrefix

interface FileInstanceFactory {

    val schemes: List<String>
    suspend fun buildInstance(context: Context, uri: Uri): FileInstance?

    fun getPrefix(context: Context, uri: Uri): FileSystemPrefix? = null

    fun buildNestedFile(context: Context, name: String?, fileInstance: FileInstance): Uri? = null
}

@Suppress("unused")
suspend fun getFileInstance(
    context: Context,
    uri: Uri,
    policy: FileCreatePolicy = FileCreatePolicy.NotCreate
) = getFactory(uri) { safeUri ->
    buildInstance(context, safeUri)
}?.apply {
    if (policy is FileCreatePolicy.Create && !exists()) {
        if (policy.isFile) {
            createFile()
        } else {
            createDirectory()
        }
    }
}

suspend fun getFileSystemPrefix(
    context: Context,
    uri: Uri,
) = getFactory(uri) { safeUri ->
    getPrefix(context, safeUri)
}

suspend fun <R> getFactory(uri: Uri, block: suspend FileInstanceFactory.(Uri) -> R?): R? {
    val unsafePath = uri.path!!
    assert(!unsafePath.endsWith("/") || unsafePath.length == 1) {
        "invalid path [$unsafePath]"
    }
    val path = simplePath(unsafePath)
    val safeUri = uri.buildUpon().path(path).build()
    val loader = ServiceLoader.load(FileInstanceFactory::class.java)
    return loader.firstNotNullOfOrNull {
        it.block(safeUri)
    }
}

/**
 * 会针对. 和.. 特殊路径进行处理。
 * 无法正确处理toChild 时是ArchiveFileInstance 的情况
 */
@Throws(Exception::class)
suspend fun FileInstance.toChildEfficiently(
    context: Context,
    name: String,
    policy: FileCreatePolicy = FileCreatePolicy.NotCreate
): FileInstance {
    assert(name.last() != '/') {
        "$name is not a valid name"
    }
    if (name == ".") {
        return this
    }
    if (name == "..") {
        return toParentEfficiently(context)
    }
    if (fileKind().isFile) {
        val uri1 = getFactory(uri) {
            buildNestedFile(context, name, this@toChildEfficiently)
        }
        if (uri1 != null) {
            return getFileInstance(context, uri1)!!
        } else {
            throw IllegalAccessException("is file")
        }
    }
    val path = buildPath(path, name)
    val childUri = uri.buildUpon().path(path).build()

    val currentPrefix = getFileSystemPrefix(context, uri)
    val childPrefix = getFileSystemPrefix(context, childUri)
    return if (currentPrefix == childPrefix) {
        toChild(name, policy)!!
    } else {
        getFileInstance(context, childUri, policy)!!
    }
}

@Throws(Exception::class)
suspend fun FileInstance.toParentEfficiently(
    context: Context
): FileInstance {
    val parentPath = parentPath(path)
    val parentUri = uri.buildUpon().path(parentPath).build()

    val parentPrefix = getFileSystemPrefix(context, parentUri)
    val childPrefix = getFileSystemPrefix(context, uri)
    return if (parentPrefix == childPrefix) {
        toParent()
    } else {
        getFileInstance(context, parentUri)!!
    }
}
