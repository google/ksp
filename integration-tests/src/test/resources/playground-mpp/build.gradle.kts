plugins {
    kotlin("multiplatform") apply false
}

val testRepo: String by project
subprojects {
    repositories {
        mavenLocal()
        maven(testRepo)
        mavenCentral()
        google()
    }
}

