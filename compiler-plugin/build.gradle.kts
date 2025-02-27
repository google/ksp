import com.google.devtools.ksp.RelativizingPathProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

evaluationDependsOn(":common-util")

description = "Kotlin Symbol Processing"

val intellijVersion: String by project
val kotlinBaseVersion: String by project

val junitVersion: String by project
val junit5Version: String by project
val junitPlatformVersion: String by project
val libsForTesting: Configuration by configurations.creating
val libsForTestingCommon: Configuration by configurations.creating

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
        from(project(":common-util").sourceSets.main.get().allSource)
    }
}

// WARNING: remember to update the dependencies in symbol-processing as well.
dependencies {
    listOf(
        "com.jetbrains.intellij.platform:util-rt",
        "com.jetbrains.intellij.platform:util-class-loader",
        "com.jetbrains.intellij.platform:util-text-matching",
        "com.jetbrains.intellij.platform:util",
        "com.jetbrains.intellij.platform:util-base",
        "com.jetbrains.intellij.platform:util-xml-dom",
        "com.jetbrains.intellij.platform:core",
        "com.jetbrains.intellij.platform:core-impl",
        "com.jetbrains.intellij.platform:extensions",
        "com.jetbrains.intellij.java:java-frontback-psi",
        "com.jetbrains.intellij.java:java-frontback-psi-impl",
        "com.jetbrains.intellij.java:java-psi",
        "com.jetbrains.intellij.java:java-psi-impl",
    ).forEach {
        implementation("$it:$intellijVersion") { isTransitive = false }
    }

    implementation(kotlin("stdlib", kotlinBaseVersion))

    compileOnly("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(project(":api"))
    implementation(project(":common-util"))

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-suite:$junitPlatformVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinBaseVersion")
    testImplementation(project(":test-utils"))

    libsForTesting(kotlin("stdlib", kotlinBaseVersion))
    libsForTesting(kotlin("test", kotlinBaseVersion))
    libsForTesting(kotlin("script-runtime", kotlinBaseVersion))
    libsForTestingCommon(kotlin("stdlib-common", kotlinBaseVersion))
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    from(project(":common-util").tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

val copyLibsForTesting by tasks.registering(Copy::class) {
    from(configurations["libsForTesting"])
    into("dist/kotlinc/lib")
    val escaped = Regex.escape(kotlinBaseVersion)
    rename("(.+)-$escaped\\.jar", "$1.jar")
}

val copyLibsForTestingCommon by tasks.registering(Copy::class) {
    from(configurations["libsForTestingCommon"])
    into("dist/common")
    val escaped = Regex.escape(kotlinBaseVersion)
    rename("(.+)-$escaped\\.jar", "$1.jar")
}

tasks.test {
    dependsOn(copyLibsForTesting)
    dependsOn(copyLibsForTestingCommon)
    maxHeapSize = "2g"

    useJUnitPlatform()

    systemProperty("idea.is.unit.test", "true")
    systemProperty("java.awt.headless", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")

    testLogging {
        events("passed", "skipped", "failed")
    }

    val ideaHomeDir = layout.buildDirectory.dir("tmp/ideaHome")
        .get()
        .asFile
        .apply { if (!exists()) mkdirs() }
    jvmArgumentProviders.add(RelativizingPathProvider("idea.home.path", ideaHomeDir))
    jvmArgumentProviders.add(RelativizingPathProvider("java.io.tmpdir", temporaryDir))
}
