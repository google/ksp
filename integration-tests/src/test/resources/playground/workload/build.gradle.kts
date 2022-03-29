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
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(project(":test-processor"))
    ksp(project(":test-processor"))
}

ksp {
    arg("option1", "value1")
    arg("option2", "value2")
}

val compileKotlin: AbstractKotlinCompileTool<*> by tasks
tasks.register<Copy>("copyG") {
    from("G.kt")
    into(File(buildDir, "generatedSources").apply { mkdirs() })
}.let {
    // Magic. `map` creates a provider to propagate task dependency.
    compileKotlin.setSource(it.map { it.destinationDir })
}
