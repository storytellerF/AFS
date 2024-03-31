package com.storyteller_f.file_system_local

import android.net.Uri
import com.storyteller_f.file_system.getFileInstance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class FileInstanceTest {
//    @Config(minSdk = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    fun testFakeEmulatedPath() {
        val uri = Uri.parse("file:///storage/emulated")
        val context = RuntimeEnvironment.getApplication()
        runBlocking {
            assert(getFileInstance(context, uri)!!.list().count > 0)
        }
    }
}
