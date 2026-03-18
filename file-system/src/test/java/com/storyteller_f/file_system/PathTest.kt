package com.storyteller_f.file_system

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.io.encoding.ExperimentalEncodingApi

class PathTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testBuildPath() {
        assertEquals("/a/b", buildPath("a", "b"))
        assertEquals("/a/b/c", buildPath("a", "b", "c"))
        assertEquals("/a.txt", buildPath("a.txt"))
        assertEquals("/a/b/c", buildPath("a", "b/", "/c"))
    }

    @Test
    fun testParentPath() {
        assertEquals("/a", parentPath("a", "b"))
        assertEquals("/", parentPath("a"))
        assertNull(parentPath("/"))
        assertEquals("/a/b", parentPath("a", "b", "c"))
    }

    @Test
    fun testGetExtension() {
        assertEquals("txt", getExtension("file.txt"))
        assertEquals("gz", getExtension("archive.tar.gz"))
        assertNull(getExtension("fileWithoutExtension"))
        assertEquals("", getExtension("fileWithDotAtEnd."))
    }

    @Test
    fun testSimplePath() {
        assertEquals("/a/b", simplePath("/a/b"))
        assertEquals("/a/b", simplePath("/a//b"))
        assertEquals("/a", simplePath("/a/b/.."))
        assertEquals("/", simplePath("/a/.."))
        assertEquals("/a/c", simplePath("/a/b/../c"))
        assertEquals("/a/b", simplePath("/a/./b"))
        assertEquals("/", simplePath("/"))
    }

    @Test
    fun testEnsureFile() = runBlocking {
        val newFile = File(tempFolder.root, "test_dir/test_file.txt")
        val result = newFile.ensureFile()
        assertEquals(newFile, result)
        assertTrue(newFile.exists())
        assertTrue(newFile.isFile)
    }

    @Test
    fun testEnsureDirs() = runBlocking {
        val newDir = File(tempFolder.root, "test_dir/sub_dir")
        val result = newDir.ensureDirs()
        assertEquals(newDir, result)
        assertTrue(newDir.exists())
        assertTrue(newDir.isDirectory)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun testBase64EncodingDecoding() {
        val original = "test_string_123"
        val encoded = original.encodeByBase64()
        val decoded = encoded.decodeByBase64()
        assertEquals(original, decoded)
    }
}
