import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
}

// As per https://docs.gradle.org/7.2/userguide/validation_problems.html#implementation_unknown
tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        apiVersion = "1.4"
        languageVersion = "1.4"
    }
}
