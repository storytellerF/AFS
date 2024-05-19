package com.storyteller_f.file_system

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SeekableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.file.AccessMode
import java.nio.file.CopyOption
import java.nio.file.DirectoryStream
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import java.util.ServiceLoader

@RequiresApi(Build.VERSION_CODES.O)
fun installedProviders(): List<FileSystemProvider> {
    return ServiceLoader.load(FileSystemProvider::class.java).asIterable().toList()
}

@RequiresApi(Build.VERSION_CODES.O)
fun newFileSystem(path: Path, env: Map<String, *>): FileSystem? {
    return installedProviders().first {
        it.scheme == path.toUri().scheme
    }.newFileSystem(path, env)
}

@RequiresApi(Build.VERSION_CODES.O)
fun newFileSystem(uri: URI, env: Map<String, *>): FileSystem? {
    return installedProviders().first {
        it.scheme == uri.scheme
    }.newFileSystem(uri, env)
}

fun URI.toUri(): Uri {
    return Uri.Builder().scheme(scheme).authority(authority).path(path).build()
}

// archive://ZmlsZTovLy9kYXRhL3VzZXIvMC9jb20uc3Rvcnl0ZWxsZXJfZi5maWxlX3N5c3RlbV9hcmNoaXZlLnRlc3QvZmlsZXMvdGVzdC56aXA=/
fun Uri.toURI(): URI {
    return URI(scheme, authority, path, null, null)
}

@RequiresApi(Build.VERSION_CODES.O)
class AFSPath(val uri: URI, val context: Context, private val provider: FileSystemProvider) : Path {

    private val scheme: String = uri.scheme!!
    private val authority: String? = uri.authority
    val instance = runBlocking {
        getFileInstance(context, uri.toUri())
    }

    override fun compareTo(other: Path?): Int {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<Path> {
        TODO("Not yet implemented")
    }

    override fun register(
        watcher: WatchService?,
        events: Array<out WatchEvent.Kind<*>>?,
        vararg modifiers: WatchEvent.Modifier?
    ): WatchKey {
        TODO("Not yet implemented")
    }

    override fun register(watcher: WatchService?, vararg events: WatchEvent.Kind<*>?): WatchKey {
        TODO("Not yet implemented")
    }

    override fun getFileSystem(): FileSystem {
        return AFS(scheme, authority.orEmpty(), context, provider)
    }

    override fun isAbsolute(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getRoot(): Path {
        TODO("Not yet implemented")
    }

    override fun getFileName(): Path {
        TODO("Not yet implemented")
    }

    override fun getParent(): Path {
        TODO("Not yet implemented")
    }

    override fun getNameCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getName(index: Int): Path {
        TODO("Not yet implemented")
    }

    override fun subpath(beginIndex: Int, endIndex: Int): Path {
        TODO("Not yet implemented")
    }

    override fun startsWith(other: Path?): Boolean {
        TODO("Not yet implemented")
    }

    override fun startsWith(other: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun endsWith(other: Path?): Boolean {
        TODO("Not yet implemented")
    }

    override fun endsWith(other: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun normalize(): Path {
        TODO("Not yet implemented")
    }

    override fun resolve(other: Path?): Path {
        TODO("Not yet implemented")
    }

    override fun resolve(other: String?): Path {
        TODO("Not yet implemented")
    }

    override fun resolveSibling(other: Path?): Path {
        TODO("Not yet implemented")
    }

    override fun resolveSibling(other: String?): Path {
        TODO("Not yet implemented")
    }

    override fun relativize(other: Path?): Path {
        TODO("Not yet implemented")
    }

    override fun toUri(): URI {
        TODO("Not yet implemented")
    }

    override fun toAbsolutePath(): Path {
        TODO("Not yet implemented")
    }

    override fun toRealPath(vararg options: LinkOption?): Path {
        TODO("Not yet implemented")
    }

    override fun toFile(): File {
        TODO("Not yet implemented")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class AFS(
    val scheme: String,
    private val authority: String,
    val context: Context,
    private val provider: FileSystemProvider
) : FileSystem() {
    override fun close() {
        // do nothing
    }

    override fun provider(): FileSystemProvider {
        return provider
    }

    override fun isOpen(): Boolean {
        return true
    }

    override fun isReadOnly(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSeparator(): String {
        return "/"
    }

    override fun getRootDirectories(): MutableIterable<Path> {
        TODO("Not yet implemented")
    }

    override fun getFileStores(): MutableIterable<FileStore> {
        TODO("Not yet implemented")
    }

    override fun supportedFileAttributeViews(): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun getPath(first: String?, vararg more: String?): Path {
        return AFSPath(
            URI(
                scheme,
                authority,
                buildPath(first!!, *more.filterNotNull().toTypedArray()),
                null,
                null
            ),
            context,
            provider
        )
    }

    override fun getPathMatcher(syntaxAndPattern: String?): PathMatcher {
        TODO("Not yet implemented")
    }

    override fun getUserPrincipalLookupService(): UserPrincipalLookupService {
        TODO("Not yet implemented")
    }

    override fun newWatchService(): WatchService {
        TODO("Not yet implemented")
    }
}

@RequiresApi(Build.VERSION_CODES.O)
abstract class AFSProvider : FileSystemProvider() {

    override fun newFileSystem(uri: URI?, env: Map<String, *>?): FileSystem {
        require(uri!!.scheme == scheme)
        val context1 = env?.get(CONTEXT_KEY) as Context
        return AFS(scheme, uri.authority.orEmpty(), context1, this)
    }

    override fun getFileSystem(uri: URI?): FileSystem {
        TODO("Not yet implemented")
    }

    override fun getPath(uri: URI?): Path {
        return getFileSystem(uri!!).getPath(uri.path)
    }

    override fun newByteChannel(
        path: Path?,
        options: Set<OpenOption>?,
        vararg attrs: FileAttribute<*>?
    ): SeekableByteChannel {
        return AFSFileChannel(path as AFSPath)
    }

    override fun newDirectoryStream(
        dir: Path?,
        filter: DirectoryStream.Filter<in Path>?
    ): DirectoryStream<Path> {
        TODO("Not yet implemented")
    }

    override fun createDirectory(dir: Path?, vararg attrs: FileAttribute<*>?) {
        TODO("Not yet implemented")
    }

    override fun delete(path: Path?) {
        TODO("Not yet implemented")
    }

    override fun copy(source: Path?, target: Path?, vararg options: CopyOption?) {
        TODO("Not yet implemented")
    }

    override fun move(source: Path?, target: Path?, vararg options: CopyOption?) {
        TODO("Not yet implemented")
    }

    override fun isSameFile(path: Path?, path2: Path?): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHidden(path: Path?): Boolean {
        TODO("Not yet implemented")
    }

    override fun getFileStore(path: Path?): FileStore {
        TODO("Not yet implemented")
    }

    override fun checkAccess(path: Path?, vararg modes: AccessMode?) {
        TODO("Not yet implemented")
    }

    override fun <V : FileAttributeView?> getFileAttributeView(
        path: Path?,
        type: Class<V>?,
        vararg options: LinkOption?
    ): V {
        TODO("Not yet implemented")
    }

    override fun <A : BasicFileAttributes?> readAttributes(
        path: Path?,
        type: Class<A>?,
        vararg options: LinkOption?
    ): A {
        TODO("Not yet implemented")
    }

    override fun readAttributes(
        path: Path?,
        attributes: String?,
        vararg options: LinkOption?
    ): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override fun setAttribute(
        path: Path?,
        attribute: String?,
        value: Any?,
        vararg options: LinkOption?
    ) {
        TODO("Not yet implemented")
    }

    companion object {
        const val CONTEXT_KEY = "context"
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class AFSFileChannel(path: AFSPath) : FileChannel() {

    val instance = path.instance!!
    private val inputStream by lazy {
        runBlocking {
            instance.getInputStream()
        }
    }
    private var position = 0L
    private val outputStream by lazy {
        runBlocking {
            instance.getOutputStream()
        }
    }

    override fun implCloseChannel() {
        inputStream.close()
        outputStream.close()
    }

    override fun read(dst: ByteBuffer?): Int {
        val byteArray = ByteArray(1024)
        val read = inputStream.read(byteArray)
        if (read != -1) {
            dst?.put(byteArray, 0, read)
            position += read
        }
        return read
    }

    override fun read(dsts: Array<out ByteBuffer>?, offset: Int, length: Int): Long {
        TODO("Not yet implemented")
    }

    override fun read(dst: ByteBuffer?, position: Long): Int {
        TODO("Not yet implemented")
    }

    override fun write(src: ByteBuffer?): Int {
        TODO("Not yet implemented")
    }

    override fun write(srcs: Array<out ByteBuffer>?, offset: Int, length: Int): Long {
        TODO("Not yet implemented")
    }

    override fun write(src: ByteBuffer?, position: Long): Int {
        TODO("Not yet implemented")
    }

    override fun position(): Long {
        return position
    }

    override fun position(newPosition: Long): FileChannel {
        TODO("Not yet implemented")
    }

    override fun size(): Long {
        return runBlocking {
            instance.size()
        }
    }

    override fun truncate(size: Long): FileChannel {
        TODO("Not yet implemented")
    }

    override fun force(metaData: Boolean) {
        TODO("Not yet implemented")
    }

    override fun transferTo(position: Long, count: Long, target: WritableByteChannel?): Long {
        TODO("Not yet implemented")
    }

    override fun transferFrom(src: ReadableByteChannel?, position: Long, count: Long): Long {
        TODO("Not yet implemented")
    }

    override fun map(mode: MapMode?, position: Long, size: Long): MappedByteBuffer {
        TODO("Not yet implemented")
    }

    override fun lock(position: Long, size: Long, shared: Boolean): FileLock {
        TODO("Not yet implemented")
    }

    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock {
        TODO("Not yet implemented")
    }
}
