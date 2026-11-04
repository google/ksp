buildscript {
    val testRepo: String by project
    val kotlinVersion: String by project

    repositories {
        maven(testRepo)
        google()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}

plugins {
    id("com.android.application") apply false
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
