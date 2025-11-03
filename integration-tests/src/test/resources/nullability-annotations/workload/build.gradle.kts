import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompileTool

val testRepo: String by project

plugins {
    id("com.google.devtools.ksp")
    kotlin("jvm")
}

version = "1.0-SNAPSHOT"

repositories {
    maven(testRepo)
    mavenCentral()
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":test-processor"))
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.checkerframework:checker-qual:1.8.1")
    implementation("org.jetbrains:annotations:26.0.2-1")
    ksp(project(":test-processor"))
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}

val compileKotlin: AbstractKotlinCompileTool<*> by tasks
tasks.register<Copy>("copyG") {
    from("G.kt")
    into(layout.buildDirectory.file("generatedSources"))
}.let {
    // Magic. `map` creates a provider to propagate task dependency.
    compileKotlin.source(it.map { it.destinationDir })
}
