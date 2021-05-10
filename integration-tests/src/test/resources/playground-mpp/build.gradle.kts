plugins {
    kotlin("multiplatform") apply false
}

val testRepo: String by project
subprojects {
    repositories {
        maven(testRepo)
        mavenCentral()
        google()
    }
}

