import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "6.0.0"
    `maven-publish`
}

val packedJars by configurations.creating

dependencies {
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
            pom {
                name.set("com.google.devtools.ksp:symbol-processing")
                description.set("Symbol processing for Kotlin")
            }
        }
        project.shadow.component(publication)
    }
}
