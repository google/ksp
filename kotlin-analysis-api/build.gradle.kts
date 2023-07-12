import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.devtools.ksp.RelativizingPathProvider

description = "Kotlin Symbol Processing implementation using Kotlin Analysis API"

val junitVersion: String by project
val junit5Version: String by project
val junitPlatformVersion: String by project
val libsForTesting by configurations.creating
val libsForTestingCommon by configurations.creating
val signingKey: String? by project
val signingPassword: String? by project

val aaKotlinBaseVersion: String by project
val aaIntellijVersion: String by project
val aaGuavaVersion: String by project
val aaAsmVersion: String by project
val aaFastutilVersion: String by project

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("com.github.johnrengelman.shadow")
    `maven-publish`
    signing
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
        implementation("$it:$aaIntellijVersion") { isTransitive = false }
    }

    listOf(
        "org.jetbrains.kotlin:high-level-api-fir-for-ide",
        "org.jetbrains.kotlin:high-level-api-for-ide",
        "org.jetbrains.kotlin:low-level-api-fir-for-ide",
        "org.jetbrains.kotlin:analysis-api-providers-for-ide",
        "org.jetbrains.kotlin:analysis-project-structure-for-ide",
        "org.jetbrains.kotlin:symbol-light-classes-for-ide",
        "org.jetbrains.kotlin:analysis-api-standalone-for-ide",
        "org.jetbrains.kotlin:high-level-api-impl-base-for-ide",
        "org.jetbrains.kotlin:kotlin-compiler-for-ide",
    ).forEach {
        implementation("$it:$aaKotlinBaseVersion") { isTransitive = false }
    }

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")
    implementation(kotlin("stdlib", aaKotlinBaseVersion))

    implementation("com.google.guava:guava:$aaGuavaVersion")
    implementation("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil:$aaFastutilVersion")
    implementation("org.jetbrains.intellij.deps:asm-all:$aaAsmVersion")

    implementation(project(":api"))
    implementation(project(":common-util"))

    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit5Version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-params:$junit5Version")
    testRuntimeOnly("org.junit.platform:junit-platform-suite:$junitPlatformVersion")

    testImplementation(project(":test-utils"))
    testImplementation(project(":api"))
    testImplementation(project(":common-util"))

    testImplementation(kotlin("stdlib", aaKotlinBaseVersion))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler:$aaKotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$aaKotlinBaseVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-scripting-compiler:$aaKotlinBaseVersion")

    libsForTesting(kotlin("stdlib", aaKotlinBaseVersion))
    libsForTesting(kotlin("test", aaKotlinBaseVersion))
    libsForTesting(kotlin("script-runtime", aaKotlinBaseVersion))
    libsForTestingCommon(kotlin("stdlib-common", aaKotlinBaseVersion))
}

tasks.register<Copy>("CopyLibsForTesting") {
    from(configurations.get("libsForTesting"))
    into("dist/kotlinc/lib")
    val escaped = Regex.escape(aaKotlinBaseVersion)
    rename("(.+)-$escaped\\.jar", "$1.jar")
}

tasks.register<Copy>("CopyLibsForTestingCommon") {
    from(configurations.get("libsForTestingCommon"))
    into("dist/common")
    val escaped = Regex.escape(aaKotlinBaseVersion)
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
    flatDir {
        dirs("${project.rootDir}/third_party/prebuilt/repo/")
    }
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    archiveClassifier.set("real")
}

tasks.withType<ShadowJar>() {
    archiveClassifier.set("")
    minimize()
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
    val dokkaJavadocJar by creating(Jar::class) {
        dependsOn(dokkaJavadoc)
        from(dokkaJavadoc.flatMap { it.outputDirectory })
        archiveClassifier.set("javadoc")
    }
    publish {
        dependsOn(shadowJar)
        dependsOn(sourcesJar)
        dependsOn(dokkaJavadocJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "symbol-processing-aa"
            artifact(tasks["shadowJar"])
            artifact(project(":kotlin-analysis-api").tasks["dokkaJavadocJar"])
            artifact(project(":kotlin-analysis-api").tasks["sourcesJar"])
            pom {
                name.set("com.google.devtools.ksp:symbol-processing-aa")
                description.set("KSP implementation on Kotlin Analysis API")
                withXml {
                    fun groovy.util.Node.addDependency(
                        groupId: String,
                        artifactId: String,
                        version: String,
                        scope: String = "runtime"
                    ) {
                        appendNode("dependency").apply {
                            appendNode("groupId", groupId)
                            appendNode("artifactId", artifactId)
                            appendNode("version", version)
                            appendNode("scope", scope)
                        }
                    }

                    asNode().appendNode("dependencies").apply {
                        addDependency("org.jetbrains.kotlin", "kotlin-stdlib", aaKotlinBaseVersion)
                    }
                }
            }
        }
    }
}

signing {
    isRequired = hasProperty("signingKey")
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(extensions.getByType<PublishingExtension>().publications)
}
