import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "1.4.0" apply false
}

if (!extra.has("kspVersion")) {
    val kotlinBaseVersion: String by project
    val today = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    extra.set("kspVersion", "$kotlinBaseVersion-dev-experimental-$today")
}

subprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}