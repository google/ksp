pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    }
    val kotlinBaseVersion: String = java.util.Properties().also { props ->
        settingsDir.parentFile.resolve("gradle.properties").inputStream().use {
            props.load(it)
        }
    }["kotlinBaseVersion"] as String
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.jvm") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinBaseVersion")
            }
        }
    }
}
