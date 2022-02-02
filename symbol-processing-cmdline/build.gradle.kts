import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kotlinBaseVersion: String by project
val signingKey: String? by project
val signingPassword: String? by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `maven-publish`
    signing
}

val packedJars by configurations.creating

dependencies {
    packedJars(project(":compiler-plugin")) { isTransitive = false }
    packedJars(project(":common-util")) { isTransitive = false }
}

tasks.withType<ShadowJar>() {
    archiveClassifier.set("")
    // ShadowJar picks up the `compile` configuration by default and pulls stdlib in.
    // Therefore, specifying another configuration instead.
    configurations = listOf(packedJars)
}

tasks {
    publish {
        dependsOn(shadowJar)
        dependsOn(project(":compiler-plugin").tasks["dokkaJavadocJar"])
    }

    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(project(":compiler-plugin").sourceSets.main.get().allSource)
        from(project(":common-util").sourceSets.main.get().allSource)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "symbol-processing-cmdline"
            artifact(tasks["sourcesJar"])
            artifact(project(":compiler-plugin").tasks["dokkaJavadocJar"])
            artifact(tasks["shadowJar"])
            pom {
                name.set("com.google.devtools.ksp:symbol-processing-cmdline")
                description.set("Symbol processing for K/N and command line")
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey") && !gradle.taskGraph.hasTask("publishToMavenLocal")
    sign(extensions.getByType<PublishingExtension>().publications)
}
