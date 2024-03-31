package com.storyteller_f.file_system

import android.content.ContentResolver
import android.net.Uri
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.FileKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.LinkedList
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 返回的即是canonical path，也是absolute path
 */
fun buildPath(vararg part: String) = part.fold("/") { acc, s ->
    simplePath("$acc/$s")
}

fun parentPath(vararg part: String): String? {
    val currentPath = buildPath(*part)
    if (currentPath == "/") return null
    val endIndex = if (currentPath.last() == '/') {
        currentPath.lastIndex - 1
    } else {
        currentPath.lastIndex
    }
    val index = currentPath.lastIndexOf("/", endIndex)
    if (index == 0) return "/"
    return currentPath.substring(0, index)
}

fun getExtension(name: String): String? {
    val index = name.lastIndexOf('.')
    return if (index == -1) null else name.substring(index + 1)
}

suspend fun File.ensureFile(): File? {
    if (!exists()) {
        parentFile?.ensureDirs() ?: return null
        if (!withContext(Dispatchers.IO) {
                createNewFile()
            }) {
            return null
        }
    }
    return this
}

suspend fun File.ensureDirs(): File? {
    if (!exists()) {
        if (!withContext(Dispatchers.IO) {
                mkdirs()
            }) {
            return null
        }
    }
    return this
}

/**
 * 简化路径。
 */
fun simplePath(path: String): String {
    assert(path[0] == '/') {
        "$path is not valid"
    }
    val stack = LinkedList<String>()
    var position = 1
    stack.add("/")
    val nameStack = LinkedList<Char>()
    while (position < path.length) {
        val current = path[position++]
        checkPath(current, stack, nameStack)
    }
    val s = nameStack.joinToString("")
    if (s.isNotEmpty()) {
        if (s == "..") {
            if (stack.size > 1) {
                stack.removeLast()
                stack.removeLast()
            }
        } else if (s != ".") stack.add(s)
    }
    if (stack.size > 1 && stack.last == "/") stack.removeLast()
    return stack.joinToString("")
}

private fun checkPath(
    current: Char,
    stack: LinkedList<String>,
    nameStack: LinkedList<Char>
) {
    if (current != '/') {
        nameStack.add(current)
    } else if (stack.last != "/" || nameStack.size != 0) {
        val name = nameStack.joinToString("")
        nameStack.clear()
        when (name) {
            ".." -> {
                stack.removeLast()
                stack.removeLast() // 弹出上一个 name
            }

            "." -> {
                // 无效操作
            }

            else -> {
                stack.add(name)
                stack.add("/")
            }
        }
    }
}

/**
 * 通过base64 解码还原原始的authority
 */
val Uri.tree: String
    get() {
        return rawTree.decodeByBase64()
    }

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeByBase64() =
    Base64.decode(toByteArray()).decodeToString()

@OptIn(ExperimentalEncodingApi::class)
fun String.encodeByBase64() = Base64.encode(toByteArray())

/**
 * 存储原始authority 的信息，使用base64 编码
 */
val Uri.rawTree: String
    get() {
        assert(scheme == ContentResolver.SCHEME_CONTENT)
        return pathSegments.first()!!
    }

suspend fun FileInstance.size(): Long {
    return (fileKind() as FileKind.File).size
}
