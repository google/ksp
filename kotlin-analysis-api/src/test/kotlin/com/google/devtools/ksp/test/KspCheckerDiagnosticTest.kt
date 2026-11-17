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

import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.impl.KspDiagnostics
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSFileJavaImpl
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.KspGradleLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSNode
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject

@ResourceLock("diagnosticTests")
class KspCheckerDiagnosticTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun javaCheckerKeepsBasenameAndDeduplicatesSameLineErrors() {
        File(directory, "BrokenJava.java").writeText("class BrokenJava { Missing first; Missing second; }")
        val output = captureOutput {
            withResolver { resolver ->
                KspDiagnostics.runJava(resolver.javaFiles(), KspGradleLogger(3))
            }
        }
        assertEquals("e: [ksp] BrokenJava.java:1: Unresolved reference 'Missing'\n", output)
    }

    @Test
    fun javaCheckerSuppliesSourceWithoutChangingDecoratedBody() {
        val source = File(directory, "BrokenJava.java").apply {
            writeText("class BrokenJava { Missing first; Missing second; }")
        }
        val diagnostics = mutableListOf<Diagnostic<out JavaFileObject>>()
        val logger = listenerLogger(DiagnosticListener { diagnostics.add(it) })
        val output = captureOutput {
            withResolver { resolver -> KspDiagnostics.runJava(resolver.javaFiles(), logger) }
        }
        assertEquals("", output)
        assertEquals(1, diagnostics.size)
        val diagnostic = diagnostics.single()
        assertEquals("[ksp] BrokenJava.java:1: Unresolved reference 'Missing'", diagnostic.getMessage(null))
        assertEquals(source.toURI(), diagnostic.source?.toUri())
        assertEquals(1L, diagnostic.lineNumber)
        assertEquals(Diagnostic.NOPOS, diagnostic.columnNumber)
    }

    @Test
    fun javaSyntaxErrorsCarryLocations() {
        val source = File(directory, "BrokenJava.java").apply { writeText("class BrokenJava { void broken( }") }
        val messages = mutableListOf<Pair<String, KSNode?>>()
        val logger = object : KSPLogger by CommandLineKSPLogger() {
            override fun error(message: String, symbol: KSNode?) { messages.add(message to symbol) }
        }
        withResolver { resolver -> KspDiagnostics.runJava(resolver.javaFiles(), logger) }
        assertTrue(messages.isNotEmpty())
        messages.forEach { (message, symbol) ->
            assertTrue(message.startsWith("BrokenJava.java:1: "))
            assertEquals(FileLocation(source.invariantSeparatorsPath, 1), symbol?.location)
        }
    }

    @Test
    fun kotlinCheckerPreservesBasenameAndReportsIndependentSource() {
        val source = File(directory, "BrokenKotlin.kt").apply { writeText("class BrokenKotlin :\n    Missing()") }
        val diagnostics = mutableListOf<Diagnostic<out JavaFileObject>>()
        val logger = listenerLogger(DiagnosticListener { diagnostics.add(it) })
        withResolver { resolver ->
            val files = resolver.getAllFiles().filterIsInstance<KSFileImpl>().map { it.ktFileSymbol.psi as KtFile }.toList()
            KspDiagnostics.runKotlin(files, logger)
        }
        val diagnostic = diagnostics.single { it.getMessage(null).contains("Unresolved reference 'Missing'") }
        assertTrue(diagnostic.getMessage(null).startsWith("[ksp] BrokenKotlin.kt:2: "))
        assertEquals(source.toURI(), diagnostic.source?.toUri())
        assertEquals(2L, diagnostic.lineNumber)
    }

    @Test
    fun commandLineCheckerKeepsBasenameAndDeduplication() {
        File(directory, "BrokenJava.java").writeText("class BrokenJava { Missing first; Missing second; }")
        val output = captureOutput(error = true) {
            withResolver { resolver -> KspDiagnostics.runJava(resolver.javaFiles(), CommandLineKSPLogger()) }
        }
        assertEquals("BrokenJava.java:1: Unresolved reference 'Missing'\n", output)
    }

    private fun Resolver.javaFiles() = getAllFiles().filterIsInstance<KSFileJavaImpl>().map { it.psi }.toList()

    private fun listenerLogger(listener: DiagnosticListener<JavaFileObject>): KspGradleLogger {
        val constructor = KspGradleLogger::class.java.constructors.find { it.parameterCount == 2 }
        assertNotNull(constructor, "KspGradleLogger must expose its diagnostic listener overload")
        return constructor!!.newInstance(3, listener) as KspGradleLogger
    }

    private fun withResolver(action: (Resolver) -> Unit) {
        val provider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    action(resolver)
                    return emptyList()
                }
            }
        }
        assertEquals(KotlinSymbolProcessing.ExitCode.OK,
            KotlinSymbolProcessing(diagnosticTestConfig(directory), listOf(provider), KspGradleLogger(6)).execute())
    }
}
