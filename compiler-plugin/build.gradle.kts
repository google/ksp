import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

evaluationDependsOn(":common-util")

description = "Kotlin Symbol Processing"

val intellijVersion: String by project
val kotlinBaseVersion: String by project

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
        from(project(":common-util").sourceSets.main.get().allSource)
    }
}

// WARNING: remember to update the dependencies in symbol-processing as well.
dependencies {
    listOf(
        "com.jetbrains.intellij.platform:util-rt",
        "com.jetbrains.intellij.platform:util-class-loader",
        "com.jetbrains.intellij.platform:util-text-matching",
        "com.jetbrains.intellij.platform:util",
        "com.jetbrains.intellij.platform:util-base",
        "com.jetbrains.intellij.platform:util-xml-dom",
        "com.jetbrains.intellij.platform:core",
        "com.jetbrains.intellij.platform:core-impl",
        "com.jetbrains.intellij.platform:extensions",
        "com.jetbrains.intellij.java:java-psi",
        "com.jetbrains.intellij.java:java-psi-impl",
    ).forEach {
        implementation("$it:$intellijVersion") { isTransitive = false }
    }

    implementation(kotlin("stdlib", kotlinBaseVersion))

    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    implementation(project(":api"))
    implementation(project(":common-util"))
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    from(project(":common-util").tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
