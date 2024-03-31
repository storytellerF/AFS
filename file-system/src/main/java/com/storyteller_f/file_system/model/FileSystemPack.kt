package com.storyteller_f.file_system.model

class FileSystemPack(
    val files: MutableList<FileInfo>,
    val directories: MutableList<FileInfo>
) {

    fun addFiles(infoList: List<FileInfo>) {
        files.addAll(infoList)
    }

    fun addDirectories(infoList: List<FileInfo>) {
        directories.addAll(infoList)
    }

    fun addFile(info: FileInfo): Boolean {
        return files.add(info)
    }

    fun addDirectory(info: FileInfo): Boolean {
        return directories.add(info)
    }

    val count: Int
        get() = files.size + directories.size

    companion object {
        val EMPTY = FileSystemPack(mutableListOf(), mutableListOf())
    }
}
