plugins {
    `maven-publish`
}

val env: MutableMap<String, String> = System.getenv()
group = group.takeIf { it.toString().contains(".") } ?: env["GROUP"] ?: "com.storyteller_f"
version = version.takeIf { it != "unspecified" } ?: env["VERSION"] ?: "0.0.1-local"
afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                val component = components.find {
                    it.name == "java" || it.name == "release"
                }
                from(component)
            }
        }
    }
}