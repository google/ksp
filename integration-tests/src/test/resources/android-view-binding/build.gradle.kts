buildscript {
    val testRepo: String by project

    repositories {
        maven(testRepo)
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        google()
    }
}

plugins {
    kotlin("jvm") apply false
    id("com.android.application") apply false
    kotlin("android") apply false
    id("com.google.devtools.ksp") apply false
}

allprojects {
    val testRepo: String by project
    repositories {
        maven(testRepo)
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        google()
    }
}
