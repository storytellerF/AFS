@file:Suppress("UnstableApiUsage")

pluginManagement {
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
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
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
includeBuild("common-publish")
