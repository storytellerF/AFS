import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.googleKsp) apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("org.jetbrains.kotlinx.kover") version "0.9.3"
    id("com.starter.easylauncher") version ("6.2.0") apply false
}

setupDeprecationCheck(listOf(""))
setupDetekt()
setupKover(
    listOf(
        "file-system",
        "file-system-archive",
        "file-system-ktx",
        "file-system-local",
        "file-system-memory",
        "file-system-remote",
//        "file-system-root",
    ), listOf()
)
tasks.withType<Test> {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(
            TestLogEvent.STARTED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
            TestLogEvent.PASSED
        )
        showStandardStreams = true
    }
}

fun Project.setupDetekt() {
    val detektReportMergeSarif by tasks.registering(ReportMergeTask::class) {
        output = layout.buildDirectory.file("reports/detekt/merge.sarif")
    }
    subprojects {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        detekt {
            source.setFrom(
                io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_JAVA,
                io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_TEST_SRC_DIR_JAVA,
                io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_KOTLIN,
                io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_TEST_SRC_DIR_KOTLIN,
            )
            buildUponDefaultConfig = true
            autoCorrect = true
            config.setFrom("$rootDir/config/detekt/detekt.yml")
            baseline = file("$rootDir/config/detekt/baseline.xml")
        }
        dependencies {
            val detektVersion = "1.23.1"

            detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
            detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:$detektVersion")
            detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:$detektVersion")
        }
        tasks.withType<Detekt>().configureEach {
            jvmTarget = "1.8"
            reports {
                xml.required = true
                html.required = true
                txt.required = true
                sarif.required = true
                md.required = true
            }
            basePath = rootDir.absolutePath
            finalizedBy(detektReportMergeSarif)
        }
        detektReportMergeSarif {
            input.from(
                tasks.withType<Detekt>().map { it.sarifReportFile })
        }
        tasks.withType<DetektCreateBaselineTask>().configureEach {
            jvmTarget = "1.8"
        }
    }
}

fun Project.setupKover(
    androidLibModules: List<String>,
    jvmLibModules: List<String>
) {
    dependencies {
        val action = { it: String ->
            kover(project(":$it"))
            Unit
        }
        androidLibModules.forEach(action)
        jvmLibModules.forEach(action)
    }
    subprojects {
        apply(plugin = "org.jetbrains.kotlinx.kover")
        //虽然模块本身已经设置了com.android.library 但是这里依然需要添加
        if (androidLibModules.contains(name)) {
            apply(plugin = "com.android.library")
        }

        dependencies {
            if (androidLibModules.contains(name)) {
                val robolectricVersion = "4.11.1"
                "testImplementation"("org.robolectric:robolectric:$robolectricVersion")
            }
        }
        kover {
            reports {
                // filters for all report types of all build variants
                filters {
                    excludes {
                        androidGeneratedClasses()
                    }
                }
            }
        }
    }
}

fun Project.setupDeprecationCheck(deprecationCheckModules: List<String>) {
    subprojects {
        if (deprecationCheckModules.contains(name)) {
            tasks.withType<KotlinCompile> {
                compilerOptions {
                    freeCompilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
                }
            }
            tasks.withType<JavaCompile> {
                options.compilerArgs =
                    options.compilerArgs + listOf("-Xlint:deprecation", "-Xlint:unchecked")
            }
        }
    }
}