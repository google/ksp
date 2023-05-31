import com.google.devtools.ksp.RelativizingPathProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

evaluationDependsOn(":common-util")

description = "Kotlin Symbol Processing"

val intellijVersion: String by project
val kotlinBaseVersion: String by project

val libsForTesting by configurations.creating
val libsForTestingCommon by configurations.creating

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij")
    id("org.jetbrains.dokka")
}

intellij {
    version = intellijVersion
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
    implementation(kotlin("stdlib", kotlinBaseVersion))
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    implementation(project(":api"))
    implementation(project(":common-util"))

    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-scripting-compiler:$kotlinBaseVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-suite:1.8.2")

    testImplementation(project(":test-utils"))

    libsForTesting(kotlin("stdlib", kotlinBaseVersion))
    libsForTesting(kotlin("test", kotlinBaseVersion))
    libsForTesting(kotlin("script-runtime", kotlinBaseVersion))
    libsForTestingCommon(kotlin("stdlib-common", kotlinBaseVersion))
}

tasks.register<Copy>("CopyLibsForTesting") {
    from(configurations.get("libsForTesting"))
    into("dist/kotlinc/lib")
    val escaped = Regex.escape(kotlinBaseVersion)
    rename("(.+)-$escaped\\.jar", "$1.jar")
}

tasks.register<Copy>("CopyLibsForTestingCommon") {
    from(configurations.get("libsForTestingCommon"))
    into("dist/common")
    val escaped = Regex.escape(kotlinBaseVersion)
    rename("(.+)-$escaped\\.jar", "$1.jar")
}

fun Project.javaPluginConvention(): JavaPluginConvention = the()
val JavaPluginConvention.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")
val Project.testSourceSet: SourceSet
    get() = javaPluginConvention().testSourceSet

tasks.test {
    dependsOn("CopyLibsForTesting")
    dependsOn("CopyLibsForTestingCommon")
    maxHeapSize = "2g"

    useJUnitPlatform()

    systemProperty("idea.is.unit.test", "true")
    systemProperty("java.awt.headless", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")

    testLogging {
        events("passed", "skipped", "failed")
    }

    lateinit var tempTestDir: File
    doFirst {
        val ideaHomeDir = buildDir.resolve("tmp/ideaHome").takeIf { it.exists() || it.mkdirs() }!!
        jvmArgumentProviders.add(RelativizingPathProvider("idea.home.path", ideaHomeDir))

        tempTestDir = createTempDir()
        jvmArgumentProviders.add(RelativizingPathProvider("java.io.tmpdir", tempTestDir))
    }

    doLast {
        delete(tempTestDir)
    }
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    maven("https://www.jetbrains.com/intellij-repository/snapshots")
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    from(project(":common-util").tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
