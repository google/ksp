import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

description = "Kotlin Symbol Processor"

val junitVersion: String by project
val signingKey: String? by project
val signingPassword: String? by project

plugins {
    kotlin("jvm")
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
    id("com.google.devtools.ksp")
}

dependencies {
    compileOnly(project(":api"))
    testImplementation("junit:junit:$junitVersion")

    ksp(project(":cmdline-parser-gen"))
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(project.sourceSets.main.map { it.allSource })
}
val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
}

kotlin {
    compilerOptions {
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "symbol-processing-common-deps"
            from(components["java"])
            artifact(sourcesJar)
            artifact(dokkaJavadocJar)
            pom {
                name.set("com.google.devtools.ksp:symbol-processing-common-deps")
                description.set("Kotlin Symbol processing Gradle Utils")
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}
