val kspVersion: String by project

plugins {
    kotlin("jvm")
}
dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation(files("/Users/alexgolubev/Projects/alex-ksp/gradle-plugin/build/kotlin/compileTestKotlin/classpath-snapshot"))
    implementation(files("/Users/alexgolubev/Projects/alex-ksp/gradle-plugin/build/classes/kotlin/test"))
    implementation(files("/Users/alexgolubev/Projects/alex-ksp/gradle-plugin/build/kotlin/compileTestKotlin/cacheable"))
}
