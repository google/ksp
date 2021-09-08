import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processing API"

val signingKey: String? by project
val signingPassword: String? by project

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version ("1.5.0")
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "symbol-processing-api"
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["dokkaJavadocJar"])
            pom {
                name.set("com.google.devtools.ksp:symbol-processing-api")
                description.set("Symbol processing for Kotlin")
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey") && !gradle.taskGraph.hasTask("publishToMavenLocal")
    sign(extensions.getByType<PublishingExtension>().publications)
}
