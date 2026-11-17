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

import com.google.devtools.ksp.gradle.testing.TestConfig
import groovy.json.JsonSlurper
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class DiagnosticsIntegrationTestFixture {
    @Rule @JvmField val tmpDir = TemporaryFolder()

    private val config = TestConfig.read()

    protected fun kspProblems() = problems().filter { it["contextualLabel"].toString().contains("[ksp]") }

    protected fun problems(): List<Map<String, Any?>> {
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

    protected fun sourceFile(name: String) =
        tmpDir.root
            .resolve(
                if (name.startsWith("Kotlin")) "app/src/main/kotlin/test/Symbols.kt"
                else "app/src/main/java/test/Symbols.java"
            )
            .canonicalFile

    protected fun runner() =
        GradleRunner.create()
            .withProjectDir(tmpDir.root)
            .withArguments(":app:classes", "--warning-mode=none", "--console=plain", "--stacktrace")

    protected fun setup(kgpVersion: String, compilerVersion: String, errors: Boolean = false) {
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
                    println("ENGINE_DIAGNOSTIC_LISTENER=" + listener?.javaClass?.simpleName?.removeSuffix("_Decorated"))
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
                                if (name.startsWith("Many")) {
                                    environment.logger.warn("repeated diagnostic", declaration)
                                    environment.logger.error("repeated diagnostic", declaration)
                                }
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

    protected fun write(path: String, content: String) {
        File(tmpDir.root, path).apply { parentFile.mkdirs() }.writeText(content.trimIndent())
    }

    companion object {
        val names = listOf("KotlinOne", "KotlinTwo", "JavaOne", "JavaTwo")
    }
}
