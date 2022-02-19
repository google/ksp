buildscript {
    val testRepo: String by project

    repositories {
        maven(testRepo)
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
        google()
    }
}

plugins {
    id("com.android.application") apply false
    kotlin("android") apply false
    id("com.google.devtools.ksp") apply false
    id("com.android.library") apply false
}

allprojects {
    val testRepo: String by project
    repositories {
        maven(testRepo)
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
        google()
    }
}
