description = "Dummy Artifact for KotlinCompilerPluginSupportPlugin"

val signingKey: String? by project
val signingPassword: String? by project
val kotlinBaseVersion: String by project

plugins {
    kotlin("jvm")
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "symbol-processing"
            pom {
                name.set("com.google.devtools.ksp:symbol-processing")
                description.set("Symbol processing for Kotlin")
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}
