plugins {
    id("com.android.library")

    id("com.google.devtools.ksp")

    id("common-publish")
    id("common-library")
}

android {
    namespace = "com.storyteller_f.file_system_remote"
}

configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.guava:listenablefuture") {
        select("com.google.guava:guava:0")
    }
}
dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.common.ktx)
    implementation(libs.slim.ktx)
    implementation(project(":file-system"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.okhttp)
    testImplementation(libs.mockwebserver)
    implementation(libs.commons.net)
    testImplementation(libs.mock.ftp.server)
    implementation(libs.smbj)
    implementation(libs.sshj)
    implementation(libs.prov)
    loadSardine()

    testImplementation(libs.mockk.android)
    testImplementation(libs.mockk.agent)
    testImplementation(libs.jimfs)
    testImplementation(libs.logback.android)
}

fun DependencyHandlerScope.loadSardine() {
    val project = findProject(":sardine-android")
    if (project != null) {
        implementation(project)
    } else {
//        implementation("com.github.storytellerF:sardine-android:7da4aa36e1")
        implementation("com.github.thegrizzlylabs:sardine-android:0.9")
    }
}

