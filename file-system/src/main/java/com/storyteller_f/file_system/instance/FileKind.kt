package com.storyteller_f.file_system.instance

sealed class SymbolicLinkType(open val origin: String) {
    data class Soft(override val origin: String) : SymbolicLinkType(origin)
    data class Hard(override val origin: String) : SymbolicLinkType(origin)

    val isSoft get() = this is Soft
    val isHard get() = this is Hard
}

sealed class FileKind(open val linkType: SymbolicLinkType? = null, open val isHidden: Boolean) {

    /**
     * @param extension 不可为null，默认是空字符串。由文件名提供的extension，并非文件本身确实是此类型。比如一个jpeg 图片，但是当前没有后缀名，extension 依然是空。
     */
    data class File(
        override val linkType: SymbolicLinkType? = null,
        override val isHidden: Boolean,
        val size: Long,
        val extension: String
    ) : FileKind(linkType, isHidden)

    data class Directory(
        override val linkType: SymbolicLinkType? = null,
        override val isHidden: Boolean
    ) : FileKind(linkType, isHidden)

    val isFile get() = this is File
    val isDirectory get() = this is Directory

    val symbolicLink: Boolean
        get() = linkType != null

    companion object {
        fun build(
            isFile: Boolean,
            isSymbolicLink: Boolean,
            isHidden: Boolean,
            size: Long,
            extension: String
        ): FileKind {
            val linkType = if (isSymbolicLink) SymbolicLinkType.Soft("") else null
            return if (isFile) File(linkType, isHidden, size, extension) else Directory(linkType, isHidden)
        }
    }
}
