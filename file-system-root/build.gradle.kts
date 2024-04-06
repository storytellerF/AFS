import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.implModule
import com.storyteller_f.version_manager.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("common-publish")
    id("com.storyteller_f.version_manager")
}

android {
    namespace = "com.storyteller_f.file_system_root"
}
baseLibrary()

dependencies {
    implementation(libs.nio)

    unitTestDependency()
    implModule(":file-system")
}