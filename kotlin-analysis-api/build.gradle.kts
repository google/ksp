import com.google.devtools.ksp.RelativizingPathProvider

description = "Kotlin Symbol Processing implementation using Kotlin Analysis API"

val intellijVersion: String by project
val junitVersion: String by project
val kotlinBaseVersion: String by project
val libsForTesting by configurations.creating
val libsForTestingCommon by configurations.creating

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij")
    id("org.jetbrains.dokka")
}

intellij {
    version = intellijVersion
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")
    implementation(kotlin("stdlib", kotlinBaseVersion))
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    implementation("org.jetbrains.kotlin:high-level-api-fir-for-ide:$kotlinBaseVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:high-level-api-for-ide:$kotlinBaseVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:low-level-api-fir-for-ide:$kotlinBaseVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:analysis-api-providers-for-ide:$kotlinBaseVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:analysis-project-structure-for-ide:$kotlinBaseVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:symbol-light-classes-for-ide:$kotlinBaseVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:analysis-api-standalone-for-ide:$kotlinBaseVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:high-level-api-impl-base-for-ide:$kotlinBaseVersion") {
        isTransitive = false
    }

    implementation(project(":api"))
    implementation(project(":common-util"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-suite:1.8.2")

    testImplementation(project(":test-utils"))
    testImplementation(project(":api"))
    testImplementation(project(":common-util"))

    testImplementation(kotlin("stdlib", kotlinBaseVersion))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-scripting-compiler:$kotlinBaseVersion")

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

sourceSets.main {
    java.srcDirs("src/main/kotlin")
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
        val ideaHomeDir = project.layout.buildDirectory.dir("tmp/ideaHome").get().asFile
        jvmArgumentProviders.add(RelativizingPathProvider("idea.home.path", ideaHomeDir))

        tempTestDir = createTempDir()
        jvmArgumentProviders.add(RelativizingPathProvider("java.io.tmpdir", tempTestDir))
    }

    doLast {
        delete(tempTestDir)
    }
}

repositories {
    flatDir {
        dirs("${project.rootDir}/third_party/prebuilt/repo/")
    }
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap/")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://www.jetbrains.com/intellij-repository/releases")
}
