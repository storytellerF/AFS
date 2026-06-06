package com.storyteller_f.file_system

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FileInstanceFactoryTest {
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun setup() {
        TestFileInstanceFactory.reset()
    }

    @Test
    fun getFileInstanceNormalizesPathAndAppliesCreatePolicy() = runBlocking {
        val uri = Uri.Builder()
            .scheme(TestFileInstanceFactory.SCHEME)
            .authority("main")
            .path("/root/./docs/../file.txt")
            .build()

        val instance = getFileInstance(context, uri, FileCreatePolicy.Create(true))!!

        assertEquals("/root/file.txt", instance.path)
        assertTrue(instance.exists())
        assertTrue(instance.fileKind().isFile)
    }

    @Test
    fun getFileSystemPrefixReturnsProviderPrefix() = runBlocking {
        val uri = Uri.Builder()
            .scheme(TestFileInstanceFactory.SCHEME)
            .authority("main")
            .path("/root")
            .build()

        assertEquals(TestFileInstanceFactory.TestPrefix("main"), getFileSystemPrefix(context, uri))
        assertNull(getFileSystemPrefix(context, Uri.Builder().scheme("missing").path("/root").build()))
    }

    @Test
    fun toChildEfficientlyHandlesSpecialNamesAndSamePrefixChildren() = runBlocking {
        val instance = getFileInstance(
            context,
            Uri.Builder().scheme(TestFileInstanceFactory.SCHEME).authority("main").path("/root").build(),
            FileCreatePolicy.Create(false)
        )!!

        assertSame(instance, instance.toChildEfficiently(context, "."))
        assertEquals("/", instance.toChildEfficiently(context, "..").path)

        val child = instance.toChildEfficiently(context, "child.txt", FileCreatePolicy.Create(true))
        assertEquals("/root/child.txt", child.path)
        assertTrue(child.exists())
        assertTrue(child.fileKind().isFile)
    }

    @Test
    fun toChildEfficientlyBuildsNestedInstanceForFiles() = runBlocking {
        val zip = getFileInstance(
            context,
            Uri.Builder().scheme(TestFileInstanceFactory.SCHEME).authority("main").path("/archive.zip").build(),
            FileCreatePolicy.Create(true)
        )!!

        val nested = zip.toChildEfficiently(context, "entry.txt")

        assertEquals(TestFileInstanceFactory.NESTED_SCHEME, nested.uri.scheme)
        assertEquals("/entry.txt", nested.path)
    }
}
