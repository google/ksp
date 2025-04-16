buildscript {
    val testRepo: String by project

    repositories {
        maven(testRepo)
        mavenCentral()
        maven("https://redirector.kotlinlang.org/maven/bootstrap/")
        google()
    }
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
