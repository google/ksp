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
}

tasks.withType<ShadowJar>() {
    archiveClassifier.set("")
    // ShadowJar picks up the `compile` configuration by default and pulls stdlib in.
    // Therefore, specifying another configuration instead.
    configurations = listOf(packedJars)
    relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")
}

tasks {
    publish {
        dependsOn(shadowJar)
        dependsOn(project(":compiler-plugin:dokkaJavadocJar"))
    }

    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(project(":compiler-plugin").sourceSets.main.get().allSource)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "symbol-processing"
            artifact(tasks["sourcesJar"])
            artifact(project(":compiler-plugin").tasks["dokkaJavadocJar"])
            artifact(tasks["shadowJar"])
            pom {
                name.set("com.google.devtools.ksp:symbol-processing")
                description.set("Symbol processing for Kotlin")
                // FIXME: figure out how to make ShadowJar generate dependencies in POM,
                //        or simply depends on kotlin-compiler-embeddable so that relocation
                //        isn't needed, at the price of giving up composite build.
                withXml {
                    fun groovy.util.Node.addDependency(groupId: String, artifactId: String, version: String, scope: String = "runtime") {
                        appendNode("dependency").apply {
                            appendNode("groupId", groupId)
                            appendNode("artifactId", artifactId)
                            appendNode("version", version)
                            appendNode("scope", scope)
                        }
                    }

                    asNode().appendNode("dependencies").apply {
                        addDependency("org.jetbrains.kotlin", "kotlin-stdlib", kotlinBaseVersion)
                        addDependency("org.jetbrains.kotlin", "kotlin-compiler-embeddable", kotlinBaseVersion)
                        addDependency("com.google.devtools.ksp", "symbol-processing-api", version)
                    }
                }
            }
        }

        create<MavenPublication>("cmdline") {
            artifactId = "symbol-processing-cmdline"
            artifact(tasks["sourcesJar"])
            artifact(project(":compiler-plugin").tasks["dokkaJavadocJar"])
            from(project(":compiler-plugin").components["java"])
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
