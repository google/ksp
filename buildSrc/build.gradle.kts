plugins {
    kotlin("jvm") version embeddedKotlinVersion
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    mavenCentral()
}
