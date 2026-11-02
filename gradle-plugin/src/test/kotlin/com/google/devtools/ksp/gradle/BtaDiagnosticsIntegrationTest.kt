/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.devtools.ksp.gradle

import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.gradle.testing.TestConfig
import groovy.json.JsonSlurper
import java.io.File
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BtaDiagnosticsIntegrationTest {
    @Rule @JvmField val tmpDir = TemporaryFolder()

    private val config = TestConfig.read()

    @Test
    fun compiler24UsesTheIsolatedListenerAndCompilesGeneratedSources() {
        setup("2.4.0", "2.4.0")
        val result = runner().build()
        assertCompilerVersion(result, "2.4.0")
        assertListener(result, true)
        assertDiagnostics(result)
        assertThat(result.task(":app:compileKotlin")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(tmpDir.root.resolve("app/build/classes/kotlin/main/test/Generated.class").isFile).isTrue()
        val records = tmpDir.root.resolve("diagnostics.txt").readLines()
        for (name in names) {
            assertThat(records).contains("WARNING|[ksp] ${sourceFile(name).invariantSeparatorsPath}:2: warning-$name|${sourceFile(name).toURI()}|2|-1")
        }
        assertThat(records).contains("WARNING|[ksp] source-free warning|null|-1|-1")
    }

    @Test
    fun enabledErrorsRetainTheirOutputBeforeTheWorkerFails() {
        setup("2.4.0", "2.4.0", errors = true)
        val result = runner().buildAndFail()
        assertListener(result, true)
        assertFailure(result)
        assertDiagnostics(result, errors = true)
        val records = tmpDir.root.resolve("diagnostics.txt").readLines()
        assertThat(records).contains("ERROR|[ksp] source-free error|null|-1|-1")
        assertThat(records).contains("ERROR|[ksp] Symbols.kt:3: Unresolved reference 'Generated'.|${sourceFile("KotlinOne").toURI()}|3|-1")
    }

    @Test
    fun fallbackErrorsRetainTheirOutputBeforeTheWorkerFails() {
        setup("2.3.20", "2.3.20", errors = true)
        val result = runner().buildAndFail()
        assertListener(result, false)
        assertFailure(result)
        assertDiagnostics(result, errors = true)
    }

    @Test
    fun warningsAsErrorsFailWithoutChangingWarningRendering() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("app/build.gradle.kts").appendText("\nksp { allWarningsAsErrors = true }\n")
        val result = runner().buildAndFail()
        assertFailure(result)
        assertDiagnostics(result, failed = true)
    }

    @Test
    fun workerStartupErrorsArePrintedOnce() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider")
            .writeText("")
        val result = runner().buildAndFail()
        assertThat(result.task(":app:kspKotlin")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output.lineSequence().filter { "[ksp]" in it }.toList())
            .containsExactly("e: [ksp] No providers found in processor classpath.")
        assertNoKspProblems()
    }

    @Test
    fun lateCompilerOverride23UsesTheFallback() {
        setup("2.4.0", "2.3.20")
        val buildFile = tmpDir.root.resolve("app/build.gradle.kts")
        buildFile.writeText(buildFile.readText().replace(
            "kotlin.compilerVersion.set(\"2.3.20\")",
            """
            kotlin.compilerVersion.set("2.4.0")
            kotlin.compilerVersion.finalizeValueOnRead()
            afterEvaluate {
                tasks.named("kspKotlin").get()
                kotlin.compilerVersion.set("2.3.20")
            }
            """.trimIndent(),
        ))
        val result = runner().build()
        assertCompilerVersion(result, "2.3.20")
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=false")
        assertListener(result, false)
        assertDiagnostics(result)
    }

    @Test
    fun baselineKgpUsesTheFallback() {
        setup("2.3.20", "2.3.20")
        val result = runner().build()
        assertCompilerVersion(result, "2.3.20")
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=false")
        assertListener(result, false)
        assertDiagnostics(result)
    }

    @Test
    fun configurationCacheRetainsBtaSelectionAfterCompilerVersionChanges() {
        setup("2.4.0", "2.4.0")
        val cached = runner().withArguments(":app:classes", "--console=plain", "--configuration-cache",
            "--configuration-cache-problems=fail", "--rerun-tasks")
        val enabled = cached.build()
        assertThat(enabled.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertListener(enabled, true)
        assertDiagnostics(enabled)

        val buildFile = tmpDir.root.resolve("app/build.gradle.kts")
        val original = buildFile.readText()
        buildFile.writeText(original.replace("\"2.4.0\"", "\"2.3.20\""))
        val disabled = cached.build()
        assertCompilerVersion(disabled, "2.3.20")
        assertThat(disabled.output).contains("DIAGNOSTICS_BTA_ENABLED=false")
        assertListener(disabled, false)
        assertDiagnostics(disabled)
        val disabledReuse = cached.build()
        assertThat(disabledReuse.output).contains("Reusing configuration cache.")
        assertThat(disabledReuse.output).contains("DIAGNOSTICS_BTA_ENABLED=false")
        assertListener(disabledReuse, false)
        assertDiagnostics(disabledReuse)

        buildFile.writeText(original)
        val reenabled = cached.build()
        assertCompilerVersion(reenabled, "2.4.0")
        assertThat(reenabled.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertListener(reenabled, true)
        assertDiagnostics(reenabled)
        val enabledReuse = cached.build()
        assertThat(enabledReuse.output).contains("Reusing configuration cache.")
        assertCompilerVersion(enabledReuse, "2.4.0")
        assertThat(enabledReuse.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertListener(enabledReuse, true)
        assertDiagnostics(enabledReuse)
    }

    @Test
    fun localPropertiesDoNotConfigureKspDiagnostics() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("local.properties").writeText("kotlin.compiler.runViaBuildToolsApi=false\n")
        val result = runner().build()
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertListener(result, true)
        assertDiagnostics(result)
    }

    @Test
    fun extraPropertiesDoNotOverrideKspDiagnostics() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("gradle.properties").appendText("\nkotlin.compiler.runViaBuildToolsApi=true\n")
        tmpDir.root.resolve("app/build.gradle.kts").appendText("\nextra[\"kotlin.compiler.runViaBuildToolsApi\"] = false\n")
        val result = runner().build()
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertListener(result, true)
        assertDiagnostics(result)
    }

    @Test
    fun gradlePropertyCanDisableKspBtaDiagnostics() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("gradle.properties").appendText("\nkotlin.compiler.runViaBuildToolsApi=false\n")
        val result = runner().build()
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=false")
        assertListener(result, false)
        assertDiagnostics(result)
    }

    @Test
    fun commandLinePropertyOverridesGradleProperties() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("gradle.properties").appendText("\nkotlin.compiler.runViaBuildToolsApi=false\n")
        val result = runner().withArguments(":app:classes", "--console=plain",
            "-Pkotlin.compiler.runViaBuildToolsApi=true").build()
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertListener(result, true)
        assertDiagnostics(result)
    }

    @Test
    fun configurationCacheReusesTheListenerAndQuietErrorsRemainVisible() {
        setup("2.4.0", "2.4.0")
        val cached = runner().withArguments(":app:classes", "--console=plain", "--configuration-cache",
            "--configuration-cache-problems=fail", "--rerun-tasks")
        assertDiagnostics(cached.build())
        val reused = cached.build()
        assertThat(reused.output).contains("Reusing configuration cache.")
        assertThat(reused.output).doesNotContain("FIXTURE_CONFIGURED")
        assertListener(reused, true)
        assertDiagnostics(reused)
        write("app/src/main/java/test/ErrorTrigger.java", "package test; class ErrorTrigger {}")
        val failed = cached.withArguments(cached.arguments + "--quiet").buildAndFail()
        assertFailure(failed)
        val quietOutput = failed.output.lineSequence().filter { "[ksp]" in it }.toList()
        assertThat(quietOutput).contains("e: [ksp] ${tmpDir.root.resolve("app/src/main/java/test/ErrorTrigger.java").canonicalFile.invariantSeparatorsPath}:1: quiet-cache error")
        assertNoKspProblems()
        val fallbackArguments = cached.arguments.filterNot { it == "--quiet" } +
            "-Pkotlin.compiler.runViaBuildToolsApi=false"
        assertListener(cached.withArguments(fallbackArguments).buildAndFail(), false)
        val fallback = cached.withArguments(fallbackArguments + "--quiet").buildAndFail()
        assertThat(fallback.output).doesNotContain("FIXTURE_CONFIGURED")
        assertListener(fallback, false)
        assertFailure(fallback)
        assertThat(fallback.output.lineSequence().filter { "[ksp]" in it }.toList())
            .containsExactlyElementsIn(quietOutput)
        assertNoKspProblems()
    }

    @Test
    fun kspDiagnosticsOptOutRetainsCompilerSelectionAndLegacyOutput() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("gradle.properties").appendText("\nksp.diagnostics.useBuildToolsApi=false\n")
        val result = runner().build()
        assertListener(result, false)
        assertCompilerVersion(result, "2.4.0")
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertThat(tmpDir.root.resolve("diagnostics.txt").exists()).isFalse()
        assertDiagnostics(result)
    }

    @Test
    fun kspDiagnosticsOptOutSupportsCommandLineOverridesAndConfigurationCacheReuse() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("gradle.properties").appendText("\nksp.diagnostics.useBuildToolsApi=false\n")
        val arguments = listOf(":app:classes", "--console=plain", "--configuration-cache",
            "--configuration-cache-problems=fail", "--rerun-tasks")
        val cached = runner().withArguments(arguments + "-Pksp.diagnostics.useBuildToolsApi=true")
        assertListener(cached.build(), true)
        val enabledReuse = cached.build()
        assertThat(enabledReuse.output).contains("Reusing configuration cache.")
        assertListener(enabledReuse, true)
        assertDiagnostics(enabledReuse)
        val disabled = cached.withArguments(arguments).build()
        assertListener(disabled, false)
        assertCompilerVersion(disabled, "2.4.0")
        assertThat(disabled.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertDiagnostics(disabled)
        val disabledReuse = cached.build()
        assertThat(disabledReuse.output).contains("Reusing configuration cache.")
        assertListener(disabledReuse, false)
        assertDiagnostics(disabledReuse)
        write("app/src/main/java/test/ErrorTrigger.java", "package test; class ErrorTrigger {}")
        val failed = cached.buildAndFail()
        assertThat(failed.output).contains("Reusing configuration cache.")
        assertListener(failed, false)
        assertFailure(failed)
        val expected = names.map { "w: [ksp] ${sourceFile(it).invariantSeparatorsPath}:2: warning-$it" } + listOf(
            "w: [ksp] source-free warning",
            "e: [ksp] ${tmpDir.root.resolve("app/src/main/java/test/ErrorTrigger.java").canonicalFile.invariantSeparatorsPath}:1: quiet-cache error",
            "e: [ksp] Symbols.kt:3: Unresolved reference 'Generated'.",
        )
        assertThat(failed.output.lineSequence().filter { "[ksp]" in it }.toList())
            .containsExactlyElementsIn(expected)
        assertNoKspProblems()
    }

    @Test
    fun kspDiagnosticsOptInDoesNotOverrideCompilerBtaOptOut() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("gradle.properties").appendText(
            "\nkotlin.compiler.runViaBuildToolsApi=false\nksp.diagnostics.useBuildToolsApi=true\n")
        val result = runner().build()
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=false")
        assertListener(result, false)
        assertDiagnostics(result)
    }

    private fun assertCompilerVersion(result: BuildResult, version: String) {
        assertThat(result.output).contains("RESOLVED_COMPILER=$version")
    }

    private fun assertListener(result: BuildResult, enabled: Boolean) {
        assertThat(result.output).contains("ENGINE_DIAGNOSTIC_LISTENER=${if (enabled) "BtaKspDiagnosticReporter" else "null"}")
        if (enabled) assertThat(result.output).contains("ENGINE_LOGGER_ISOLATED=true")
    }

    private fun assertFailure(result: BuildResult) {
        assertThat(result.task(":app:kspKotlin")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("KSP failed with exit code: PROCESSING_ERROR")
    }

    private fun assertDiagnostics(result: BuildResult, errors: Boolean = false, failed: Boolean = errors) {
        val expected = names.map { "w: [ksp] ${sourceFile(it).invariantSeparatorsPath}:2: warning-$it" } +
            listOf("w: [ksp] source-free warning") +
            (if (errors) names.map { "e: [ksp] ${sourceFile(it).invariantSeparatorsPath}:2: error-$it" } +
                listOf("e: [ksp] source-free error") else emptyList()) +
            (if (failed) listOf("e: [ksp] Symbols.kt:3: Unresolved reference 'Generated'.") else emptyList())
        assertThat(result.output.lineSequence().filter { "[ksp]" in it }.toList())
            .containsExactlyElementsIn(expected)
        assertNoKspProblems()
    }

    private fun assertNoKspProblems() {
        assertThat(problems().filter { it["contextualLabel"].toString().contains("[ksp]") }).isEmpty()
    }

    private fun problems(): List<Map<String, Any?>> {
        val report = tmpDir.root.resolve("build/reports/problems/problems-report.html")
        if (!report.isFile) return emptyList()
        val json =
            report
                .readText()
                .substringAfter("// begin-report-data\n")
                .substringBefore("\n// end-report-data")
        @Suppress("UNCHECKED_CAST")
        return (JsonSlurper().parseText(json) as Map<String, Any?>)["diagnostics"]
            as List<Map<String, Any?>>
    }

    private fun sourceFile(name: String) =
        tmpDir.root
            .resolve(
                if (name.startsWith("Kotlin")) "app/src/main/kotlin/test/Symbols.kt"
                else "app/src/main/java/test/Symbols.java"
            )
            .canonicalFile

    private fun runner() =
        GradleRunner.create()
            .withProjectDir(tmpDir.root)
            .withArguments(":app:classes", "--warning-mode=all", "--console=plain", "--stacktrace")

    private fun setup(kgpVersion: String, compilerVersion: String, errors: Boolean = false) {
        write(
            "settings.gradle.kts",
            """
            pluginManagement {
                repositories {
                    maven("${config.mavenRepoPath}")
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            rootProject.name = "bta-diagnostics"
            include(":app", ":processor")
        """,
        )
        write(
            "build.gradle.kts",
            """
            plugins {
                id("org.jetbrains.kotlin.jvm") version "$kgpVersion" apply false
                id("com.google.devtools.ksp") version "${config.kspVersion}" apply false
            }
            println("FIXTURE_CONFIGURED")
            subprojects {
                repositories {
                    maven("${config.mavenRepoPath}")
                    mavenCentral()
                }
            }
        """,
        )
        write(
            "gradle.properties",
            """
            kotlin.jvm.target.validation.mode=warning
            org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=768m
            kotlin.compiler.execution.strategy=in-process
        """,
        )
        write(
            "processor/build.gradle.kts",
            """
            plugins { kotlin("jvm") }
            dependencies {
                implementation("com.google.devtools.ksp:symbol-processing-api:${config.kspVersion}")
            }
        """,
        )
        write(
            "processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider",
            "test.DiagnosticProcessorProvider",
        )
        write(
            "processor/src/main/kotlin/test/DiagnosticProcessorProvider.kt",
            """
            package test
            import com.google.devtools.ksp.processing.*
            import com.google.devtools.ksp.symbol.*

            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            class DiagnosticProcessorProvider : SymbolProcessorProvider {
                override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
                    val delegate = environment.logger.javaClass.declaredFields.first {
                        KSPLogger::class.java.isAssignableFrom(it.type)
                    }
                    delegate.isAccessible = true
                    val logger = delegate.get(environment.logger) as KSPLogger
                    val listenerField = logger.javaClass.getDeclaredField("diagnosticListener")
                    listenerField.isAccessible = true
                    @Suppress("UNCHECKED_CAST")
                    val listener = listenerField.get(logger) as? javax.tools.DiagnosticListener<javax.tools.JavaFileObject>
                    println("ENGINE_DIAGNOSTIC_LISTENER=" + listener?.javaClass?.simpleName)
                    println("ENGINE_LOGGER_ISOLATED=" + (logger.javaClass.classLoader !== listener?.javaClass?.classLoader))
                    if (listener != null) {
                        listenerField.set(logger, javax.tools.DiagnosticListener<javax.tools.JavaFileObject> { diagnostic ->
                            val record = listOf(diagnostic.kind, diagnostic.getMessage(null), diagnostic.source?.toUri(),
                                diagnostic.lineNumber, diagnostic.columnNumber).joinToString("|")
                            java.io.File(environment.options.getValue("diagnosticsFile")).appendText(record + "\n")
                            listener.report(diagnostic)
                        })
                    }
                    return object : SymbolProcessor {
                        private var processed = false
                        override fun process(resolver: Resolver): List<KSAnnotated> {
                            if (processed) return emptyList()
                            processed = true
                            for (declaration in resolver.getDeclarationsFromPackage("test")) {
                                val name = declaration.simpleName.asString()
                                if (name == "ErrorTrigger") {
                                    environment.logger.error("quiet-cache error", declaration)
                                }
                                if (name in setOf("KotlinOne", "KotlinTwo", "JavaOne", "JavaTwo")) {
                                    environment.logger.warn("warning-" + name, declaration)
                                    if (environment.options["errors"] == "true") {
                                        environment.logger.error("error-" + name, declaration)
                                    }
                                }
                            }
                            environment.logger.warn("source-free warning")
                            if (environment.options["errors"] == "true") environment.logger.error("source-free error")
                            environment.codeGenerator.createNewFile(Dependencies.ALL_FILES, "test", "Generated")
                                .bufferedWriter().use { it.write("package test; class Generated") }
                            return emptyList()
                        }
                    }
                }
            }
        """,
        )
        write(
            "app/build.gradle.kts",
            """
            @file:OptIn(
                org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi::class,
                org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class,
            )
            import com.google.devtools.ksp.gradle.KspAATask
            plugins {
                kotlin("jvm")
                id("com.google.devtools.ksp")
            }
            dependencies { add("ksp", project(":processor")) }
            ksp {
                arg("errors", "$errors")
                arg("diagnosticsFile", "${tmpDir.root.resolve("diagnostics.txt").invariantSeparatorsPath}")
            }
            kotlin.compilerVersion.set("$compilerVersion")
            configurations.matching { it.name.startsWith("kotlinCompilerPluginClasspath") }.configureEach {
                resolutionStrategy.eachDependency {
                    if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-scripting-")) {
                        useVersion("$compilerVersion")
                    }
                }
            }
            val compilerArtifacts = configurations.named("kotlinBuildToolsApiClasspath")
                .get().incoming.artifacts.resolvedArtifacts
            tasks.withType<KspAATask>().configureEach {
                inputs.property("resolvedCompilerVersion", compilerArtifacts.map { artifacts ->
                    artifacts.map { it.id.componentIdentifier }
                        .filterIsInstance<org.gradle.api.artifacts.component.ModuleComponentIdentifier>()
                        .single { it.group == "org.jetbrains.kotlin" && it.module == "kotlin-compiler-embeddable" }
                        .version
                })
                doFirst {
                    println("DIAGNOSTICS_BTA_ENABLED=" + (this as KspAATask).useBuildToolsApi.get())
                    println("RESOLVED_COMPILER=" + inputs.properties["resolvedCompilerVersion"])
                }
            }
        """,
        )
        write(
            "app/src/main/kotlin/test/Symbols.kt",
            """
            package test
            class KotlinOne; class KotlinTwo
            class UsesGenerated { val value = Generated() }
        """,
        )
        write(
            "app/src/main/java/test/Symbols.java",
            """
            package test;
            class JavaOne {} class JavaTwo {}
        """,
        )
    }

    private fun write(path: String, content: String) {
        File(tmpDir.root, path).apply { parentFile.mkdirs() }.writeText(content.trimIndent())
    }

    companion object {
        private val names = listOf("KotlinOne", "KotlinTwo", "JavaOne", "JavaTwo")
    }
}
