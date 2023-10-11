import com.google.devtools.ksp.RelativizingPathProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val intellijVersion: String by project
val junitVersion: String by project
val junit5Version: String by project
val junitPlatformVersion: String by project
val kotlinBaseVersion: String by project
val libsForTesting by configurations.creating
val libsForTestingCommon by configurations.creating

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all-compatibility")
}
plugins {
    kotlin("jvm")
}

version = "2.0.255-SNAPSHOT"

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

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
        "com.jetbrains.intellij.java:java-psi",
        "com.jetbrains.intellij.java:java-psi-impl",
    ).forEach {
        implementation("$it:$intellijVersion") { isTransitive = false }
    }
    implementation(project(":api"))
    implementation(project(":compiler-plugin"))
    implementation(project(":kotlin-analysis-api"))

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")
    implementation("junit:junit:$junitVersion")
    implementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    runtimeOnly("org.junit.jupiter:junit-jupiter-params:$junit5Version")
    runtimeOnly("org.junit.platform:junit-platform-suite:$junitPlatformVersion")

    implementation(kotlin("stdlib", kotlinBaseVersion))
    implementation(project(":common-deps"))
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinBaseVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler:$kotlinBaseVersion")

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
