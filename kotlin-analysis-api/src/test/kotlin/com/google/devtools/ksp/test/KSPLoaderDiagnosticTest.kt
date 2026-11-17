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
package com.google.devtools.ksp.test

import com.google.devtools.ksp.impl.KSPLoader
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject

@ResourceLock("diagnosticTests")
class KSPLoaderDiagnosticTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun listenerOverloadDeliversProcessorErrorsAndRetainsExitCode() {
        File(directory, "Input.kt").writeText("class Input")
        val diagnostics = mutableListOf<Diagnostic<out JavaFileObject>>()
        val listener = DiagnosticListener<JavaFileObject> { diagnostics.add(it) }
        val provider = SymbolProcessorProvider { environment ->
            object : SymbolProcessor {
                override fun process(resolver: com.google.devtools.ksp.processing.Resolver) =
                    emptyList<com.google.devtools.ksp.symbol.KSAnnotated>().also {
                        environment.logger.error("processor failed", resolver.getAllFiles().single())
                    }
            }
        }
        val overload = KSPLoader::class.java.methods.find {
            it.name == "loadAndRunKSP" && it.parameterCount == 4
        }
        assertNotNull(overload, "KSPLoader must expose its diagnostic listener overload")
        var exitCode = -1
        val output = captureOutput {
            exitCode = overload!!.invoke(null, configBytes(), listOf(provider), 3, listener) as Int
        }
        assertEquals(1, exitCode)
        assertEquals("", output)
        assertTrue(diagnostics.any { it.kind == Diagnostic.Kind.ERROR &&
            it.getMessage(null).endsWith("Input.kt:1: processor failed") })
        val diagnostic = diagnostics.first { it.getMessage(null).endsWith("processor failed") }
        assertEquals(File(directory, "Input.kt").toURI(), diagnostic.source!!.toUri())
        assertEquals(1L, diagnostic.lineNumber)
    }

    @Test
    fun threeArgumentEntryPointRetainsLegacyConsole() {
        File(directory, "Input.kt").writeText("class Input")
        val provider = SymbolProcessorProvider { environment ->
            object : SymbolProcessor {
                override fun process(resolver: com.google.devtools.ksp.processing.Resolver) =
                    emptyList<com.google.devtools.ksp.symbol.KSAnnotated>().also {
                        environment.logger.warn("processor warning", resolver.getAllFiles().single())
                    }
            }
        }
        var exitCode = -1
        val output = captureOutput { exitCode = KSPLoader.loadAndRunKSP(configBytes(), listOf(provider), 3) }
        assertEquals(0, exitCode)
        assertEquals("w: [ksp] ${File(directory, "Input.kt").invariantSeparatorsPath}:1: processor warning\n", output)
    }

    private fun configBytes(): ByteArray {
        val config = diagnosticTestConfig(directory)
        return ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { it.writeObject(config) }
            bytes.toByteArray()
        }
    }
}

internal fun diagnosticTestConfig(directory: File): KSPJvmConfig = KSPJvmConfig.Builder().apply {
    moduleName = "diagnostics"
    sourceRoots = listOf(directory)
    javaSourceRoots = listOf(directory)
    jdkHome = File(System.getProperty("java.home"))
    jvmTarget = "17"
    languageVersion = "2.0"
    apiVersion = "2.0"
    projectBaseDir = directory
    outputBaseDir = File(directory, "output")
    classOutputDir = File(outputBaseDir, "classes")
    kotlinOutputDir = File(outputBaseDir, "kotlin")
    javaOutputDir = File(outputBaseDir, "java")
    resourceOutputDir = File(outputBaseDir, "resources")
    cachesDir = File(outputBaseDir, "caches")
}.build()
