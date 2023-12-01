import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

evaluationDependsOn(":kotlin-analysis-api")

val kotlinBaseVersion: String by project
val signingKey: String? by project
val signingPassword: String? by project

val aaKotlinBaseVersion: String by project
val aaIntellijVersion: String by project

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
    signing
}

val packedJars by configurations.creating

dependencies {
    packedJars(project(":kotlin-analysis-api", "shadow")) { isTransitive = false }
}

tasks.withType<Jar> {
    archiveClassifier.set("real")
}

val prefixesToRelocate = listOf(
    "org.jetbrains.kotlin." to "ksp.org.jetbrains.kotlin.",
    "com.intellij." to "ksp.com.intellij.",
)

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    // ShadowJar picks up the `compile` configuration by default and pulls stdlib in.
    // Therefore, specifying another configuration instead.
    configurations = listOf(packedJars)
    prefixesToRelocate.forEach { (f, t) ->
        relocate(f, t)
    }
    mergeServiceFiles()
}

fun String.replaceWithKsp() =
    prefixesToRelocate.fold(this) { acc, (f, t) ->
        acc.replace("package $f", "package $t")
            .replace("import $f", "import $t")
    }

val DEP_SOURCES_DIR = "$buildDir/source-jar"

tasks {
    val copyDeps by creating(Copy::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        project(":kotlin-analysis-api").configurations.getByName("depSourceJars").resolve().forEach {
            from(zipTree(it))
        }
        into("$DEP_SOURCES_DIR/ksp")
    }
    val sourcesJar by creating(Jar::class) {
        dependsOn(copyDeps)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        archiveClassifier.set("sources")
        from(project(":kotlin-analysis-api").sourceSets.main.get().allSource)
        from(project(":common-util").sourceSets.main.get().allSource)
        from(DEP_SOURCES_DIR)
        filter { it.replaceWithKsp() }
    }
    publish {
        dependsOn(shadowJar)
        dependsOn(sourcesJar)
        dependsOn(project(":kotlin-analysis-api").tasks["dokkaJavadocJar"])
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "symbol-processing-aa-embeddable"
            artifact(project(":kotlin-analysis-api").tasks["dokkaJavadocJar"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["shadowJar"])
            pom {
                name.set("com.google.devtools.ksp:symbol-processing-aa-embeddable")
                description.set("KSP implementation on Kotlin Analysis API")
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
                        addDependency("org.jetbrains.kotlin", "kotlin-stdlib", aaKotlinBaseVersion)
                        addDependency("com.google.devtools.ksp", "common-deps", version)
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
