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
    namespace = "com.storyteller_f.file_system_memory"
}

baseLibrary()
android {
    defaultConfig {
        //todo 解决java.nio.Path 的问题
        minSdk = 26
    }
}
configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.guava:listenablefuture") {
        select("com.google.guava:guava:0")
    }
}
implModule(":file-system")
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.jimfs)
    unitTestDependency()
}
constraintCommonUIListVersion(versionManager)