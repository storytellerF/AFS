plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.android.plugin.lib)
    implementation(libs.kotlin.plugin.lib)
//    implementation(libs.safeArgs.plugin.lib)
    implementation(files(libs::class.java.superclass.protectionDomain.codeSource.location))
}