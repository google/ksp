import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm") version "1.4.30-M2-104" apply false
}

if (!extra.has("kspVersion")) {
    val kotlinBaseVersion: String by project
    val today = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE)
    extra.set("kspVersion", "$kotlinBaseVersion-multiple-round-preview-$today")
}

subprojects {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}