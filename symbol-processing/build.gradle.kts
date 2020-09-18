import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Ksp - Symbol processing for Kotlin"

val kotlinBaseVersion: String by project
val kspVersion: String? by project

group = "com.google.devtools.ksp"
version = kspVersion ?: kotlinBaseVersion

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `maven-publish`
}

val packedJars by configurations.creating

dependencies {
    packedJars(project(":gradle-plugin")) { isTransitive = false }
    packedJars(project(":compiler-plugin")) { isTransitive = false }
    packedJars(project(":api")) { isTransitive = false }
}

tasks.withType<ShadowJar>() {
    archiveClassifier.set("")
    from(packedJars)
    exclude(
        "kotlin/**",
        "org/intellij/**",
        "org/jetbrains/annotations/**",
        "META-INF/maven/org.jetbrains/annotations/*",
        "META-INF/kotlin-stdlib*"
    )
    manifest.attributes.apply {
        put("Implementation-Vendor", "Google")
        put("Implementation-Title", baseName)
        put("Implementation-Version", project.version)
    }

    relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")
}

tasks {
    publish {
        dependsOn(shadowJar)
    }

    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(project(":api").sourceSets.main.get().allSource)
        from(project(":compiler-plugin").sourceSets.main.get().allSource)
        from(project(":gradle-plugin").sourceSets.main.get().allSource)
    }

    artifacts {
        archives(sourcesJar)
        archives(jar)
    }
}

publishing {
    publications {
        val publication = create<MavenPublication>("shadow") {
            artifactId = "symbol-processing"
            artifact(tasks["sourcesJar"])
        }
        project.shadow.component(publication)
        repositories {
            mavenLocal()
        }
    }
}
