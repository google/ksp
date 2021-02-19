buildscript {
    val testRepo: String by project

    repositories {
        maven(testRepo)
        mavenCentral()
        jcenter()
        google()
    }
}

allprojects {
    val testRepo: String by project
    repositories {
        maven(testRepo)
        mavenCentral()
        jcenter()
        google()
    }
}
