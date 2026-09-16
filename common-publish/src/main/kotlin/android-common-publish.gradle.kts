plugins {
    id("com.vanniktech.maven.publish")
}

println("group: $group, version: $version")

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(artifactId = project.name)

    pom {
        name.set(project.name)
        description.set("A module from AFS, an Android file system abstraction library")
        url.set("https://github.com/storytellerF/AFS")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("storytellerF")
                name.set("storytellerF")
                url.set("https://github.com/storytellerF")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/storytellerF/AFS.git")
            developerConnection.set("scm:git:ssh://github.com/storytellerF/AFS.git")
            url.set("https://github.com/storytellerF/AFS")
        }
    }
}
