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
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
    }
}

rootProject.name = "AFS"
include(":app")

include(":file-system")
include(":file-system-ktx")
include(":file-system-remote")
include(":file-system-root")
include(":file-system-memory")
include(":file-system-local")
include(":file-system-archive")
