import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.google.devtools.ksp.RelativizingInternalPathProvider
import com.google.devtools.ksp.RelativizingPathProvider
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.commons.ClassRemapper
import org.jetbrains.org.objectweb.asm.commons.Remapper
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

description = "Kotlin Symbol Processing implementation using Kotlin Analysis API"

val signingKey: String? by project
val signingPassword: String? by project

val libsForTesting: Configuration by configurations.creating
val libsForTestingCommon: Configuration by configurations.creating

val aaKotlinBaseVersion = libs.versions.aa.kotlin.base.get()

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.shadow)
    `maven-publish`
    signing
}

val depSourceJars: Configuration by configurations.creating
val depJarsForCheck: Configuration by configurations.creating
val compilerJar: Configuration by configurations.creating

val originalLog4j: Configuration by configurations.creating
val filteredLog4j = tasks.register<Jar>("filteredLog4j") {
    from(originalLog4j.map { zipTree(it) }) {
        exclude("org/apache/log4j/jdbc/**")
    }
    archiveFileName.set("log4j-filtered.jar")
}

val intellijOriginal: Configuration by configurations.creating {
    isTransitive = false
}

/**
 * Rewrites any use of `kotlinx/coroutines/internal/intellij/IntellijCoroutines` to
 * `com/intellij/util/IntelliJCoroutinesFacade` in dependencies declared in the
 * `intellijOriginal` configuration.
 */
abstract class TransformIntellijDeps : DefaultTask() {
    @get:InputFiles
    abstract val inputJars: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun transform() {
        val remapper = object : Remapper() {
            override fun map(internalName: String): String {
                if (internalName == "kotlinx/coroutines/internal/intellij/IntellijCoroutines") {
                    return "com/intellij/util/IntelliJCoroutinesFacade"
                }
                return super.map(internalName)
            }
        }

        outputDir.get().asFile.apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }

        inputJars.forEach { jarFile ->
            val outputFile = outputDir.file(jarFile.name).get().asFile
            ZipOutputStream(outputFile.outputStream()).use { zos ->
                ZipFile(jarFile).use { zip ->
                    for (entry in zip.entries()) {
                        if (entry.isDirectory) {
                            zos.putNextEntry(ZipEntry(entry.name))
                            zos.closeEntry()
                            continue
                        }
                        val bytes = zip.getInputStream(entry).readBytes()
                        val newBytes = if (entry.name.endsWith(".class")) {
                            val cr = ClassReader(bytes)
                            val cw = ClassWriter(0)
                            val cv = ClassRemapper(cw, remapper)
                            cr.accept(cv, 0)
                            cw.toByteArray()
                        } else {
                            bytes
                        }
                        zos.putNextEntry(ZipEntry(entry.name))
                        zos.write(newBytes)
                        zos.closeEntry()
                    }
                }
            }
        }
    }
}

val transformedIntellijDeps = tasks.register<TransformIntellijDeps>("transformedIntellijDeps") {
    inputJars.from(intellijOriginal)
    outputDir.set(layout.buildDirectory.dir("transformedIntellijDeps"))
}

dependencies {
    listOf(
        libs.intellij.platform.util.rt,
        libs.intellij.platform.util.classloader,
        libs.intellij.platform.util.text.matching,
        libs.intellij.platform.util.core,
        libs.intellij.platform.util.base,
        libs.intellij.platform.util.coroutines,
        libs.intellij.platform.util.xml.dom,
        libs.intellij.platform.core.main,
        libs.intellij.platform.core.impl,
        libs.intellij.platform.extensions,
        libs.intellij.platform.diagnostic.main,
        libs.intellij.platform.diagnostic.telemetry,
        libs.intellij.java.frontback.psi.main,
        libs.intellij.java.frontback.psi.impl,
        libs.intellij.java.psi.main,
        libs.intellij.java.psi.impl,
    ).forEach {
        intellijOriginal(it)
        depSourceJars(variantOf(it) { classifier("sources") }) { isTransitive = false }
    }
    implementation(files(transformedIntellijDeps.map { it.outputDir.asFileTree }))

    listOf(
        libs.aa.analysis.api.k2.ide,
        libs.aa.analysis.api.ide,
        libs.aa.low.level.api.fir.ide,
        libs.aa.analysis.api.platform.iface.ide,
        libs.aa.symbol.light.classes.ide,
        libs.aa.analysis.api.standalone.ide,
        libs.aa.analysis.api.impl.base.ide,
        libs.aa.kotlin.compiler.common.ide,
        libs.aa.kotlin.compiler.fir.ide,
        libs.aa.kotlin.compiler.fe10.ide,
        libs.aa.kotlin.compiler.ir.ide,
    ).forEach {
        implementation(it) { isTransitive = false }
        depSourceJars(variantOf(it) { classifier("sources") }) { isTransitive = false }
    }

    implementation(libs.kotlinx.collections.immutable.jvm)
    implementation(libs.kotlinx.serialization.json)
    compileOnly(libs.aa.kotlin.stdlib)

    implementation(libs.guava)
    implementation(libs.streamex)
    implementation(libs.intellij.deps.asm.all)
    implementation(libs.stax2.api) { isTransitive = false }
    implementation(libs.aalto.xml) { isTransitive = false }
    implementation(libs.caffeine)
    implementation(libs.intellij.deps.jna.core) { isTransitive = false }
    implementation(libs.intellij.deps.jna.platform) { isTransitive = false }
    implementation(libs.intellij.deps.trove4j) { isTransitive = false }
    originalLog4j(libs.intellij.deps.log4j) { isTransitive = false }
    implementation(files(filteredLog4j))
    implementation(libs.intellij.deps.jdom) { isTransitive = false }
    implementation(libs.javaslang)
    implementation(libs.javax.inject)
    implementation(libs.kotlin.reflect.legacy)
    implementation(libs.lz4.java) { isTransitive = false }
    compileOnly(libs.aa.kotlinx.coroutines.core.jvm)
    compileOnly(libs.aa.kotlinx.coroutines.core.common)
    implementation(libs.intellij.deps.fastutil) { isTransitive = false }
    implementation(libs.jetbrains.annotations)

    implementation(libs.opentelemetry.api) { isTransitive = false }

    compileOnly(project(":common-deps"))

    implementation(project(":api"))
    implementation(project(":common-util"))

    testImplementation(libs.aa.kotlin.stdlib)
    testImplementation(libs.junit4)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.platform.suite)
    testImplementation(libs.aa.kotlin.compiler.main)
    testImplementation(libs.aa.kotlin.compiler.internal.test.framework)
    testImplementation(project(":common-deps"))
    testImplementation(project(":test-utils"))
    testImplementation(libs.aa.analysis.api.test.framework)
    testImplementation(libs.aa.kotlinx.coroutines.core.jvm)
    testImplementation(libs.aa.kotlinx.coroutines.core.common)

    // See AbstractKSPTest's init block if you change any of the `libsForTesting` or `libsForTestingCommon` dependencies.
    libsForTesting(libs.aa.kotlin.stdlib)
    libsForTesting(libs.aa.kotlin.test)
    libsForTesting(libs.aa.kotlin.script.runtime)
    libsForTestingCommon(libs.aa.kotlin.stdlib.common)

    depJarsForCheck(libs.kotlin.stdlib)
    depJarsForCheck(libs.aa.kotlinx.coroutines.core.jvm)
    depJarsForCheck(libs.aa.kotlinx.coroutines.core.common)
    depJarsForCheck(project(":api"))
    depJarsForCheck(project(":common-deps"))

    compilerJar(libs.aa.kotlin.compiler.common.ide)
}

sourceSets.main {
    java.srcDirs("src/main/kotlin")
}

fun Project.javaPluginExtension(): JavaPluginExtension = the()
val JavaPluginExtension.testSourceSet: SourceSet
    get() = sourceSets.getByName("test")
val Project.testSourceSet: SourceSet
    get() = javaPluginExtension().testSourceSet

repositories {
    flatDir {
        dirs("${project.rootDir}/third_party/prebuilt/repo/")
    }
    maven("https://redirector.kotlinlang.org/maven/kotlin-ide-plugin-dependencies")
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    archiveClassifier.set("real")
}

tasks.withType<ShadowJar>().configureEach {
    dependencies {
        exclude(project(":api"))
    }
    exclude("kotlin/**")
    exclude("kotlinx/coroutines/**")
    archiveClassifier.set("")
    mergeServiceFiles()
}

abstract class ValidateShadowJar : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:InputFiles
    abstract val classpath: ConfigurableFileCollection

    @get:InputFile
    abstract val baselineFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun validate() {
        val jarJar = inputFile.get().asFile
        val depJars = classpath.files
        val stdout = ByteArrayOutputStream()
        val execResult = execOperations.exec {
            isIgnoreExitValue = true // Prevent throwing
            executable = "jdeps"
            args = listOf(
                "--multi-release", "base",
                "--missing-deps",
                "-cp", depJars.joinToString(File.pathSeparator), jarJar.path
            )
            standardOutput = stdout
        }
        if (execResult.exitValue != 0) {
            throw Exception("Unable to run jdeps")
        }

        val actualOutput = stdout.toString()
        outputFile.get().asFile.writeText(actualOutput)
        val expectedOutput = baselineFile.get().asFile.readText()
        // Compare line by line to avoid CRLF and LF mismatches
        if (actualOutput.lines() != expectedOutput.lines()) {
            throw Exception(
                """
                jdeps missing dependencies output has changed.
                Compare expected ${baselineFile.get().asFile.absolutePath} with
                actual ${outputFile.get().asFile.absolutePath}.
                """.trimIndent()
            )
        }

        JarFile(jarJar).use { jarFile ->
            jarFile.entries().asSequence().forEach {
                if (it.name.contains("org/apache/log4j/jdbc")) {
                    throw Exception(
                        """
                        Validation failed: Found unexpected package 'org/apache/log4j/jdbc' in the shadow JAR.
                        """.trimIndent()
                    )
                }
            }
        }
    }
}

val validateShadowJar = tasks.register<ValidateShadowJar>("validateShadowJar") {
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
    classpath.from(depJarsForCheck.incoming.artifactView { }.files)
    baselineFile.set(layout.projectDirectory.file("shadow-validation-baseline.txt"))
    outputFile.set(layout.buildDirectory.file("validateShadowJar.txt"))
}

tasks.named("check").configure {
    dependsOn(validateShadowJar)
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveClassifier.set("sources")
    from(sourceSets.main.map { it.allSource })
    from(project(":common-util").sourceSets.main.get().allSource)
    depSourceJars.resolve().forEach {
        from(zipTree(it))
    }
}
val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifactId = "symbol-processing-aa"
            artifact(tasks.shadowJar)
            artifact(dokkaJavadocJar)
            artifact(sourcesJar)
            pom {
                name.set("com.google.devtools.ksp:symbol-processing-aa")
                description.set("KSP implementation on Kotlin Analysis API")
                withXml {
                    fun groovy.util.Node.addDependency(
                        dependency: Provider<MinimalExternalModuleDependency>,
                        scope: String = "runtime"
                    ) {
                        val moduleDependency = dependency.get()
                        appendNode("dependency").apply {
                            appendNode("groupId", moduleDependency.module.group)
                            appendNode("artifactId", moduleDependency.module.name)
                            appendNode("version", moduleDependency.versionConstraint.requiredVersion)
                            appendNode("scope", scope)
                        }
                    }

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
                        addDependency(libs.kotlin.stdlib)
                        addDependency(libs.aa.kotlinx.coroutines.core.jvm)
                        addDependency("com.google.devtools.ksp", "symbol-processing-api", version, "compile")
                        addDependency(
                            "com.google.devtools.ksp",
                            "symbol-processing-common-deps",
                            version,
                            "compile"
                        )
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

val copyLibsForTesting = tasks.register<Copy>("copyLibsForTesting") {
    from(configurations["libsForTesting"])
    into("dist/kotlinc/lib")
    val escaped = Regex.escape(aaKotlinBaseVersion)
    rename("(.+)-$escaped\\.jar", "$1.jar")
}

val copyLibsForTestingCommon = tasks.register<Copy>("copyLibsForTestingCommon") {
    from(configurations["libsForTestingCommon"])
    into("dist/common")
    val escaped = Regex.escape(aaKotlinBaseVersion)
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
    jvmArgumentProviders.add(RelativizingInternalPathProvider("java.io.tmpdir", temporaryDir))
}
