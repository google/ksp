plugins {
    kotlin("multiplatform") apply false
    id("com.android.kotlin.multiplatform.library") apply false
}

val testRepo: String by project
allprojects {
    repositories {
        google()
        maven(testRepo)
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
    }
}
