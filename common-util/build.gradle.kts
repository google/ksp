import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

description = "Kotlin Symbol Processing Util"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
}

dependencies {
    implementation(project(":api"))
    implementation(libs.kotlin.stdlib)
    testImplementation(libs.junit4)
}

kotlin {
    compilerOptions {
        jvmDefault.set(JvmDefaultMode.ENABLE)
    }
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
}
