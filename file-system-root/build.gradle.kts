plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("common-publish")

    id("common-library")
}

android {
    namespace = "com.storyteller_f.file_system_root"
}

dependencies {
    implementation(libs.nio)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(project(":file-system"))
}