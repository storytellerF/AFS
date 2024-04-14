package com.storyteller_f.file_system_archive

import android.content.Context
import com.storyteller_f.file_system.ensureFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class Node(val name: String, val node: List<Node>, val content: String? = null)

suspend fun Context.buildZip(nodes: List<Node>, name: String): File {
    val file = File(filesDir, name).ensureFile()!!
    ZipOutputStream(file.outputStream()).use {
        nodes.forEach { node ->
            putNode(node, it, "")
        }
    }
    return file
}

private fun putNode(
    node: Node,
    stream: ZipOutputStream,
    parent: String,
) {
    if (node.content != null) {
        stream.putNextEntry(ZipEntry(parent + node.name))
        stream.write(node.content.toByteArray())
        stream.closeEntry()
    } else {
        val dName = parent + (if (node.name.endsWith("/")) node.name else "${node.name}/")
        stream.putNextEntry(ZipEntry(dName))
        stream.closeEntry()
        node.node.forEach {
            putNode(it, stream, dName)
        }
    }
}