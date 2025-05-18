import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

evaluationDependsOn(":common-util")
evaluationDependsOn(":compiler-plugin")

val kotlinBaseVersion: String by project
val signingKey: String? by project
val signingPassword: String? by project

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    `maven-publish`
    signing
}

val packedJars by configurations.creating

dependencies {
    packedJars(project(":compiler-plugin")) { isTransitive = false }
    packedJars(project(":common-util")) { isTransitive = false }
}

tasks.withType<Jar> {
    archiveClassifier.set("real")
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    // ShadowJar picks up the `compile` configuration by default and pulls stdlib in.
    // Therefore, specifying another configuration instead.
    configurations = listOf(packedJars)
    relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("sources")
    from(project(":kotlin-analysis-api").sourceSets.main.get().allSource)
}
val javadocJar = tasks.register<Jar>("javadocJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("javadoc")
    from(project(":compiler-plugin").tasks["dokkaJavadocJar"])
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "symbol-processing"
            artifact(javadocJar)
            artifact(sourcesJar)
            artifact(tasks["shadowJar"])
            pom {
                name.set("com.google.devtools.ksp:symbol-processing")
                description.set("Symbol processing for Kotlin")
                // FIXME: figure out how to make ShadowJar generate dependencies in POM,
                //        or simply depends on kotlin-compiler-embeddable so that relocation
                //        isn't needed, at the price of giving up composite build.
                withXml {
                    fun groovy.util.Node.addDependency(
                        groupId: String,
                        artifactId: String,
                        version: String,
                        scope: String = "runtime"
                    ) {
                        appendNode("dependency").apply {
                            appendNode("groupId", groupId)
                            appendNode("artifactId", artifactId)
                            appendNode("version", version)
                            appendNode("scope", scope)
                        }
                    }

                    asNode().appendNode("dependencies").apply {
                        addDependency("org.jetbrains.kotlin", "kotlin-stdlib", kotlinBaseVersion)
                        addDependency("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.6.3")
                        addDependency("com.google.devtools.ksp", "symbol-processing-api", version)
                    }
                }
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}
