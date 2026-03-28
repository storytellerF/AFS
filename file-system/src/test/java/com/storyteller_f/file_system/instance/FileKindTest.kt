package com.storyteller_f.file_system.instance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileKindTest {

    @Test
    fun testFileKindBuildFile() {
        val fileKind = FileKind.build(
            isFile = true,
            isSymbolicLink = false,
            isHidden = true,
            size = 100L,
            extension = "txt"
        )

        assertTrue(fileKind.isFile)
        assertFalse(fileKind.isDirectory)
        assertTrue(fileKind.isHidden)
        assertFalse(fileKind.symbolicLink)
        assertNull(fileKind.linkType)

        val file = fileKind as FileKind.File
        assertEquals(100L, file.size)
        assertEquals("txt", file.extension)
    }

    @Test
    fun testFileKindBuildDirectory() {
        val fileKind = FileKind.build(
            isFile = false,
            isSymbolicLink = false,
            isHidden = false,
            size = 0L,
            extension = ""
        )

        assertTrue(fileKind.isDirectory)
        assertFalse(fileKind.isFile)
        assertFalse(fileKind.isHidden)
        assertFalse(fileKind.symbolicLink)

        assertTrue(fileKind is FileKind.Directory)
    }

    @Test
    fun testFileKindBuildSymbolicLink() {
        val fileKind = FileKind.build(
            isFile = true,
            isSymbolicLink = true,
            isHidden = false,
            size = 50L,
            extension = "lnk"
        )

        assertTrue(fileKind.isFile)
        assertTrue(fileKind.symbolicLink)
        assertTrue(fileKind.linkType is SymbolicLinkType.Soft)
        assertEquals("", fileKind.linkType?.origin)
    }

    @Test
    fun testSymbolicLinkTypeProperties() {
        val softLink = SymbolicLinkType.Soft("target/path")
        val hardLink = SymbolicLinkType.Hard("target/file")

        assertTrue(softLink.isSoft)
        assertFalse(softLink.isHard)
        assertEquals("target/path", softLink.origin)

        assertTrue(hardLink.isHard)
        assertFalse(hardLink.isSoft)
        assertEquals("target/file", hardLink.origin)
    }
}
