plugins {
    kotlin("multiplatform") apply false
}

val testRepo: String by project
allprojects {
    repositories {
        maven(testRepo)
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    }
}
