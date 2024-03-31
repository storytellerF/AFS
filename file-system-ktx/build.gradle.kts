import com.storyteller_f.version_manager.apiModule
import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.constraintCommonUIListVersion
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
    namespace = "com.storyteller_f.file_system_ktx"
    defaultConfig {
        minSdk = 21
    }
    
}
baseLibrary()
dependencies {
    apiModule(":file-system")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    unitTestDependency()
}
constraintCommonUIListVersion(versionManager)