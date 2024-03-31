package com.storyteller_f.file_system_local

import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File


@RunWith(AndroidJUnit4::class)
class PermissionRequestTest {

    @get:Rule
    var mActivityRule = ActivityScenarioRule(
        AndroidTestActivity::class.java
    )

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q, maxSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRequestFilePermission() {
        val uri = File(LocalFileSystem.ROOT_USER_EMULATED_PATH).toUri()

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        mActivityRule.scenario.onActivity {
            it.lifecycleScope.launch {
                it.requestFilePermission(uri)
            }
        }

        val instance = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        instance.findObject(UiSelector().text("去授予")).click()
        instance.findObject(UiSelector().textContains("ALLOW")).click()
        instance.findObject(UiSelector().text("ALLOW")).click()
        runTest {
            assertTrue(appContext.checkFilePermission(uri))
        }

    }
}