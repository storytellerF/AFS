@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("common-publish")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven { setUrl("https://jitpack.io") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/storytellerF/common-ui-list")
            credentials {
                // 需要配置在~/.gradle/gradle.properties
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.key").get()
            }
            mavenContent {
                includeGroupAndSubgroups("com.storytellerF.common_ui_list")
            }
        }
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "AFS"
include(":app")

include(":file-system")
include(":file-system-ktx")
include(":file-system-remote")
//include(":file-system-root")
include(":file-system-memory")
include(":file-system-local")
include(":file-system-archive")
includeBuild("bgscripts")