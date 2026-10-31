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
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.KspGradleLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import javax.tools.Diagnostic
import javax.tools.DiagnosticListener
import javax.tools.JavaFileObject

@ResourceLock("diagnosticTests")
class KspGradleLoggerTest {
    @Test
    fun legacyLoggerPreservesEveryLevelAndSourceDecoration() {
        val expected = listOf(
            "v: [ksp] /src/Names.kt:3: logging\ni: [ksp] /src/Names.kt:3: info\n" +
                "w: [ksp] /src/Names.kt:3: warning\ne: [ksp] /src/Names.kt:3: error\n" +
                "e: [ksp] java.lang.IllegalStateException: broken\n",
            "i: [ksp] /src/Names.kt:3: info\nw: [ksp] /src/Names.kt:3: warning\n" +
                "e: [ksp] /src/Names.kt:3: error\ne: [ksp] java.lang.IllegalStateException: broken\n",
            "w: [ksp] /src/Names.kt:3: warning\ne: [ksp] /src/Names.kt:3: error\n" +
                "e: [ksp] java.lang.IllegalStateException: broken\n",
            "e: [ksp] /src/Names.kt:3: error\ne: [ksp] java.lang.IllegalStateException: broken\n",
            ""
        )
        listOf(0, 1, 3, 5, 6).zip(expected).forEach { (level, output) ->
            assertEquals(output, captureOutput { logEveryLevel(KspGradleLogger(level)) })
        }
    }

    @Test
    fun listenerReceivesDecoratedWarningsAndErrorsWithIndependentMetadata() {
        val diagnostics = mutableListOf<Diagnostic<out JavaFileObject>>()
        val output = captureOutput {
            val logger = listenerLogger(3, DiagnosticListener { diagnostics.add(it) })
            logEveryLevel(logger)
            logger.error("source-free")
        }
        assertEquals("", output)
        assertEquals(listOf(Diagnostic.Kind.WARNING, Diagnostic.Kind.ERROR, Diagnostic.Kind.ERROR,
            Diagnostic.Kind.ERROR), diagnostics.map { it.kind })
        val warning = diagnostics[0]
        assertEquals("[ksp] /src/Names.kt:3: warning", warning.getMessage(null))
        assertEquals(File("/src/Names.kt").toURI(), warning.source!!.toUri())
        assertEquals(3L, warning.lineNumber)
        assertEquals(Diagnostic.NOPOS, warning.columnNumber)
        assertEquals(Diagnostic.NOPOS, warning.position)
        assertEquals(Diagnostic.NOPOS, warning.startPosition)
        assertEquals(Diagnostic.NOPOS, warning.endPosition)
        assertEquals("ksp", warning.code)
        val error = diagnostics.last()
        assertEquals("[ksp] source-free", error.getMessage(null))
        assertNull(error.source)
        assertEquals(Diagnostic.NOPOS, error.lineNumber)
        assertEquals(Diagnostic.NOPOS, error.columnNumber)
        assertEquals("[ksp] java.lang.IllegalStateException: broken", diagnostics[2].getMessage(null))
    }

    @Test
    fun listenerDeliveryIgnoresConsoleFilteringAndRetainsVerboseMessages() {
        val diagnostics = mutableListOf<Diagnostic<out JavaFileObject>>()
        val output = captureOutput {
            logEveryLevel(listenerLogger(0, DiagnosticListener { diagnostics.add(it) }))
        }
        assertEquals("v: [ksp] /src/Names.kt:3: logging\ni: [ksp] /src/Names.kt:3: info\n", output)
        assertEquals(3, diagnostics.size)
        for (level in listOf(5, 6)) {
            diagnostics.clear()
            assertEquals("", captureOutput {
                logEveryLevel(listenerLogger(level, DiagnosticListener { diagnostics.add(it) }))
            })
            assertEquals(listOf(Diagnostic.Kind.WARNING, Diagnostic.Kind.ERROR, Diagnostic.Kind.ERROR),
                diagnostics.map { it.kind })
        }
    }

    @Test
    fun commandLineLoggerKeepsRawMessagesAndExceptionMessage() {
        val output = captureOutput(error = true) { logEveryLevel(CommandLineKSPLogger()) }
        assertEquals("logging\ninfo\nwarning\nerror\nbroken\n", output)
    }

    private fun logEveryLevel(logger: KSPLogger) {
        val symbol = SourceNode(FileLocation("/src/Names.kt", 3))
        logger.logging("logging", symbol)
        logger.info("info", symbol)
        logger.warn("warning", symbol)
        logger.error("error", symbol)
        logger.exception(IllegalStateException("broken"))
    }

    private fun listenerLogger(level: Int, listener: DiagnosticListener<JavaFileObject>): KspGradleLogger {
        val constructor = KspGradleLogger::class.java.constructors.find { it.parameterCount == 2 }
        assertNotNull(constructor, "KspGradleLogger must expose its diagnostic listener overload")
        return constructor!!.newInstance(level, listener) as KspGradleLogger
    }
}

internal class SourceNode(override val location: Location) : KSNode {
    override val origin: Origin = Origin.KOTLIN
    override val parent: KSNode? = null
    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = visitor.visitNode(this, data)
}

internal fun captureOutput(error: Boolean = false, action: () -> Unit): String {
    val original = if (error) System.err else System.out
    val bytes = ByteArrayOutputStream()
    val stream = PrintStream(bytes)
    try {
        if (error) System.setErr(stream) else System.setOut(stream)
        action()
        stream.flush()
        return bytes.toString(Charsets.UTF_8).replace("\r\n", "\n")
    } finally {
        if (error) System.setErr(original) else System.setOut(original)
        stream.close()
    }
}
