plugins {
    kotlin("jvm")
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/comgoogledevtools-1098/")
    }
}
