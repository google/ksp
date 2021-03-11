import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processor"

val kotlinProjectPath: String? by project
val intellijVersion: String by project
val kotlinBaseVersion: String by project
val junitVersion: String by project
val compilerTestEnabled: String by project

val libsForTesting by configurations.creating

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.6.4"
}

intellij {
    version = intellijVersion
}

fun ModuleDependency.includeJars(vararg names: String) {
    names.forEach {
        artifact {
            name = it
            type = "jar"
            extension = "jar"
        }
    }
}

// WARNING: remember to update the dependencies in symbol-processing as well.
dependencies {
    implementation(kotlin("stdlib", kotlinBaseVersion))
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    implementation(project(":api"))

    // workaround: IntelliJ doesn't resolve packed classes from included builds.
    if (kotlinProjectPath != null) {
        compileOnly("kotlin.build:intellij-core:$intellijVersion") { includeJars("intellij-core") }
        sourceArtifacts("kotlin.build:intellij-core:$intellijVersion") { includeJars("intellij-core") }
    }

    testImplementation(kotlin("stdlib", kotlinBaseVersion))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:test-infrastructure-utils:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:tests-compiler-utils:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:tests-mutes:$kotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-scripting-compiler:$kotlinBaseVersion")

    testImplementation("junit:junit:$junitVersion")

    testImplementation(project(":api"))

    libsForTesting(kotlin("stdlib", kotlinBaseVersion))
    libsForTesting(kotlin("test", kotlinBaseVersion))
    libsForTesting(kotlin("script-runtime", kotlinBaseVersion))
}

tasks.register<Copy>("CopyLibsForTesting") {
    onlyIf {
        compilerTestEnabled.toBoolean()
    }
    from(configurations.get("libsForTesting"))
    into("dist/kotlinc/lib")
    rename("(.+)-[0-9].+\\.jar", "$1.jar")
}

fun Project.javaPluginConvention(): JavaPluginConvention = the()
val JavaPluginConvention.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")
val Project.testSourceSet: SourceSet
    get() = javaPluginConvention().testSourceSet

tasks.test {
    onlyIf {
        compilerTestEnabled.toBoolean()
    }
    dependsOn("CopyLibsForTesting")
    maxHeapSize = "2g"

    systemProperty("idea.is.unit.test", "true")
    systemProperty("idea.home.path", buildDir)
    systemProperty("java.awt.headless", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("PROJECT_CLASSES_DIRS", testSourceSet.output.classesDirs.asPath)
    environment("PROJECT_BUILD_DIR", buildDir)
    testLogging {
        events("passed", "skipped", "failed")
    }

    var tempTestDir: File? = null
    doFirst {
        tempTestDir = createTempDir()
        systemProperty("java.io.tmpdir", tempTestDir.toString())
    }

    doLast {
        tempTestDir?.let { delete(it) }
    }
}

repositories {
    if (kotlinProjectPath != null) {
        ivy {
            url = uri("file:$kotlinProjectPath/dependencies/repo")
            patternLayout {
                ivy("[organisation]/[module]/[revision]/[module].ivy.xml")
                ivy("[organisation]/[module]/[revision]/ivy/[module].ivy.xml")

                artifact("[organisation]/[module]/[revision]/artifacts/lib/[artifact](-[classifier]).[ext]")
                artifact("[organisation]/[module]/[revision]/artifacts/[artifact](-[classifier]).[ext]")
                artifact("[organisation]/sources/[artifact]-[revision](-[classifier]).[ext]")
                artifact("[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]")
            }

            metadataSources {
                ivyDescriptor()
            }
        }
    }
    flatDir {
        dirs("${project.rootDir}/third_party/prebuilt/repo/")
    }
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}
