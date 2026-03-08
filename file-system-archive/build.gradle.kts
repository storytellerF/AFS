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
    androidTestImplementation(project(":file-system-local"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
