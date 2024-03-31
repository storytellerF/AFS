import com.storyteller_f.version_manager.androidTestImplModule
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
baseLibrary()
android {
    namespace = "com.storyteller_f.file_system_archive"

    defaultConfig {
        minSdk = 24
    }
    
}

dependencies {
    implModule(":file-system")
    implementation("androidx.core:core-ktx:1.12.0")
    androidTestImplModule(":file-system-local")
    unitTestDependency()
}
constraintCommonUIListVersion(versionManager)