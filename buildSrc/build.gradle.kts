plugins {
    kotlin("jvm") version embeddedKotlinVersion
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://redirector.kotlinlang.org/maven/bootstrap/")
}
