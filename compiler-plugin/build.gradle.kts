import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Symbol Processor"

group = "org.jetbrains.kotlin"
version = "1.4.0"

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs += "-Xjvm-default=compatibility"
}

plugins {
    kotlin("jvm")
    `maven-publish`
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

//val kotlinProjectPath: String? by settings
val kotlinProjectPath = "/usr/local/google/home/laszio/working/kotlin"
val intellijVersion = "193.6494.35"
val kotlinBaseVersion = "1.4.0"
val ideModuleName = "ideaIC"


dependencies {
    implementation(kotlin("stdlib", kotlinBaseVersion))
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")

    implementation(project(":api"))

    if (kotlinProjectPath != null) {
        testImplementation(kotlin("stdlib", kotlinBaseVersion))
        // workaround: IntelliJ doesn't resolve packed classes from included builds.
        implementation("kotlin.build:intellij-core:$intellijVersion") { includeJars("intellij-core") }
        sourceArtifacts("kotlin.build:intellij-core:$intellijVersion") { includeJars("intellij-core") }

        testImplementation("kotlin.build:intellij-core:$intellijVersion") { includeJars("intellij-core") }
        testRuntimeOnly("kotlin.build:$ideModuleName:$intellijVersion")
        testImplementation("kotlin.build:$ideModuleName:$intellijVersion") { includeJars("openapi", "idea", "idea_rt", "platform-api", "platform-impl", "bootstrap") }
        testImplementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinBaseVersion")
        testImplementation("org.jetbrains.kotlin:kotlin-compiler-tests:$kotlinBaseVersion")
        testImplementation("org.jetbrains.kotlin:kotlin-scripting-compiler:$kotlinBaseVersion")

        testImplementation("junit:junit:4.12")

        testImplementation(project(":api"))
    }
}

fun Project.javaPluginConvention(): JavaPluginConvention = the()
val JavaPluginConvention.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")
val Project.testSourceSet: SourceSet
    get() = javaPluginConvention().testSourceSet

tasks.test {
    maxHeapSize = "2g"

    systemProperty("idea.is.unit.test", "true")
    systemProperty("idea.home.path", "$kotlinProjectPath/dependencies/repo/kotlin.build/intellij-core/$intellijVersion/artifacts")
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
                ivy("[organisation]/$ideModuleName/[revision]/ivy/[module].ivy.xml") // bundled plugins

                artifact("[organisation]/[module]/[revision]/artifacts/lib/[artifact](-[classifier]).[ext]")
                artifact("[organisation]/[module]/[revision]/artifacts/[artifact](-[classifier]).[ext]")
                artifact("[organisation]/$ideModuleName/[revision]/artifacts/plugins/[module]/lib/[artifact](-[classifier]).[ext]") // bundled plugins
                artifact("[organisation]/sources/[artifact]-[revision](-[classifier]).[ext]")
                artifact("[organisation]/[module]/[revision]/[artifact](-[classifier]).[ext]")
            }

            metadataSources {
                ivyDescriptor()
            }
        }
    }
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

publishing {
    publications {
        create<MavenPublication>("default") {
            artifactId = "ksp-kotlin-extension"
            from(components["java"])
        }
        repositories {
            mavenLocal()
        }
    }
}
