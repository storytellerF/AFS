package com.storyteller_f.file_system

import android.content.ContextWrapper
import android.net.Uri
import androidx.startup.AppInitializer
import androidx.startup.InitializationProvider
import com.storyteller_f.file_system.instance.FileCreatePolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FileInstanceFactoryTest {
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun setup() {
        // Startup's singleton can outlive Robolectric's application between tests.
        AppInitializer::class.java.getDeclaredField("sInstance").apply {
            isAccessible = true
        }.set(null, null)
        // Robolectric does not automatically create manifest content providers.
        Robolectric.buildContentProvider(InitializationProvider::class.java).create()
        TestFileInstanceFactory.reset()
    }

    @Test
    fun startupProviderInitializesApplicationContextFromManifest() {
        assertTrue(AppInitializer.getInstance(context).isEagerlyInitialized(FileSystemInitializer::class.java))
        assertSame(context, FileSystemInitializer.applicationContext)
    }

    @Test
    fun initializerStoresApplicationContextInsteadOfWrapper() {
        val wrapper = ContextWrapper(context)
        assertSame(context, FileSystemInitializer().create(wrapper))
        assertSame(context, FileSystemInitializer.applicationContext)
    }

    @Test
    fun getFileInstanceNormalizesPathAndAppliesCreatePolicy() = runBlocking {
        val uri = Uri.Builder()
            .scheme(TestFileInstanceFactory.SCHEME)
            .authority("main")
            .path("/root/./docs/../file.txt")
            .build()

        val instance = getFileInstance(uri, FileCreatePolicy.Create(true))!!

        assertSame(context, TestFileInstanceFactory.receivedContext)
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

        assertEquals(TestFileInstanceFactory.TestPrefix("main"), getFileSystemPrefix(uri))
        assertNull(getFileSystemPrefix(Uri.Builder().scheme("missing").path("/root").build()))
    }

    @Test
    fun toChildEfficientlyHandlesSpecialNamesAndSamePrefixChildren() = runBlocking {
        val instance = getFileInstance(
            Uri.Builder().scheme(TestFileInstanceFactory.SCHEME).authority("main").path("/root").build(),
            FileCreatePolicy.Create(false)
        )!!

        assertSame(instance, instance.toChildEfficiently("."))
        assertEquals("/", instance.toChildEfficiently("..").path)

        val child = instance.toChildEfficiently("child.txt", FileCreatePolicy.Create(true))
        assertEquals("/root/child.txt", child.path)
        assertTrue(child.exists())
        assertTrue(child.fileKind().isFile)
    }

    @Test
    fun toChildEfficientlyBuildsNestedInstanceForFiles() = runBlocking {
        val zip = getFileInstance(
            Uri.Builder().scheme(TestFileInstanceFactory.SCHEME).authority("main").path("/archive.zip").build(),
            FileCreatePolicy.Create(true)
        )!!

        val nested = zip.toChildEfficiently("entry.txt")

        assertEquals(TestFileInstanceFactory.NESTED_SCHEME, nested.uri.scheme)
        assertEquals("/entry.txt", nested.path)
    }
}
