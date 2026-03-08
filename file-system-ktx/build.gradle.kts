plugins {
    id("com.android.library")

    id("com.google.devtools.ksp")

    id("common-publish")
    id("common-library")
}

android {
    namespace = "com.storyteller_f.file_system_ktx"
}

dependencies {
    implementation(project(":file-system"))
    implementation(libs.simplemagic)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
