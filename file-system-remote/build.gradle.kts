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
    namespace = "com.storyteller_f.file_system_remote"

    defaultConfig {
        minSdk = 21
    }
    
}
baseLibrary()
configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.guava:listenablefuture") {
        select("com.google.guava:guava:0")
    }
}
dependencies {
    implModule(":common-ktx")
    implModule(":slim-ktx")
    implModule(":file-system")
    unitTestDependency()
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.okhttp)
    testImplementation(libs.mockwebserver)
    // https://mvnrepository.com/artifact/commons-net/commons-net
    implementation(libs.commons.net)
    // https://mvnrepository.com/artifact/org.mockftpserver/MockFtpServer
    testImplementation(libs.mock.ftp.server)
    // https://mvnrepository.com/artifact/com.hierynomus/smbj
    implementation(libs.smbj)
    // https://mvnrepository.com/artifact/com.hierynomus/sshj
    implementation(libs.sshj)
    implementation(libs.prov)
    loadSardine()

    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.jimfs)
    testImplementation(libs.logback.android)
}
constraintCommonUIListVersion(versionManager)
fun DependencyHandlerScope.loadSardine() {
    val project = findProject(":sardine-android")
    if (project != null) {
        implementation(project)
    } else {
//        implementation("com.github.storytellerF:sardine-android:7da4aa36e1")
        implementation("com.github.thegrizzlylabs:sardine-android:0.9")
    }
}

