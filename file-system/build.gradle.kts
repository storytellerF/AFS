@file:Suppress("UnstableApiUsage")

import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.constraintCommonUIListVersion
import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.unitTestDependency

val versionManager: String by project

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.storyteller_f.version_manager")
    id("common-publish")
}

android {
    namespace = "com.storyteller_f.file_system"
    defaultConfig {
        minSdk = 16
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

}
baseLibrary(true)
implModule(":common-ktx")
implModule(":slim-ktx")
implModule(":compat-ktx")
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)

    unitTestDependency()
}
constraintCommonUIListVersion(versionManager)