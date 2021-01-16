import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processing API"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
    `maven-publish`
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    artifacts {
        archives(sourcesJar)
        archives(jar)
    }
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "symbol-processing-api"
            from(components["java"])
            artifact(tasks["sourcesJar"])
            pom {
                name.set("com.google.devtools.ksp:symbol-processing-api")
                description.set("Symbol processing for Kotlin")
            }
        }
    }
    repositories {
        maven {
            name = "test"
            url = uri("${rootProject.buildDir}/repos/test")
        }
    }
}
