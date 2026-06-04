buildscript {
    val testRepo: String by project

    repositories {
        maven(testRepo)
        google()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        mavenCentral()
    }
}

plugins {
    id("com.android.library") apply false
    kotlin("android") apply false
    id("com.google.devtools.ksp") apply false
}

allprojects {
    val testRepo: String by project
    repositories {
        maven(testRepo)
        google()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        mavenCentral()
    }
}
