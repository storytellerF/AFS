package com.storyteller_f.file_system_local

import com.storyteller_f.file_system.buildPath
import com.storyteller_f.file_system.parentPath
import com.storyteller_f.file_system.simplePath
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun testSimplePath() {
        val simplyPath = simplePath(LocalFileSystem.ROOT_USER_EMULATED_PATH)
        assertEquals(LocalFileSystem.ROOT_USER_EMULATED_PATH, simplyPath)
    }

    @Test
    fun testBuildPath() {
        assertEquals("/test/hello", buildPath("/test", "hello"))
        assertEquals(
            "/test/hello",
            buildPath("/test", "/hello")
        )
        assertEquals(
            "/test/hello",
            buildPath("/test/", "/hello")
        )
    }

    @Test
    fun testParentPath() {
        assertEquals(null, parentPath("/"))
        assertEquals("/", parentPath("/hello"))
        assertEquals("/", parentPath("/hello/"))
        assertEquals("/hello", parentPath("/hello/index.file"))
    }
}
