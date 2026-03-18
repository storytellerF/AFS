package com.storyteller_f.file_system_archive

import android.content.Context
import com.storyteller_f.file_system.ensureFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * @param name 文件或者文件夹的名称。不是绝对路径，也不是相对路径
 * @param nodes 定义文件夹的子文件或者子文件夹
 * @param content 如果内容不是null 代表这是一个文件。可以是空字符串，代表一个空的文件
 */
class Node(val name: String, val nodes: List<Node> = emptyList(), val content: String? = null)

suspend fun Context.buildZip(name: String, nodes: List<Node>): File {
    val file = File(filesDir, name).ensureFile()!!
    ZipOutputStream(file.outputStream()).use {
        nodes.forEach { node ->
            putNode("", node, it)
        }
    }
    return file
}

private fun putNode(
    parent: String,
    node: Node,
    stream: ZipOutputStream,
) {
    if (node.content != null) {
        stream.putNextEntry(ZipEntry(parent + node.name))
        stream.write(node.content.toByteArray())
        stream.closeEntry()
    } else {
        val dName = parent + (if (node.name.endsWith("/")) node.name else "${node.name}/")
        stream.putNextEntry(ZipEntry(dName))
        stream.closeEntry()
        node.nodes.forEach {
            putNode(dName, it, stream)
        }
    }
}