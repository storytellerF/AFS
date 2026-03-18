plugins {
    id("com.android.library")

    id("com.google.devtools.ksp")

    id("common-publish")
    id("common-library")
}
android {
    namespace = "com.storyteller_f.file_system_archive"
}

dependencies {
    implementation(project(":file-system"))
    implementation(libs.androidx.core.ktx)
    testImplementation(project(":file-system-local"))
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
