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
import java.io.File
import java.lang.reflect.Proxy
import java.net.URI
import javax.tools.Diagnostic
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import org.gradle.api.Action
import org.gradle.api.logging.Logger
import org.gradle.api.problems.ProblemId
import org.gradle.api.problems.ProblemReporter
import org.gradle.api.problems.ProblemSpec
import org.gradle.api.problems.Problems
import org.gradle.api.problems.Severity
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.GradleVersion
import org.junit.Test

class ProblemsKspDiagnosticReporterTest {
    @Test
    fun problemsCapabilitiesAreIndependentOfBtaSelection() {
        assertThat(supported("8.12")).isFalse()
        assertThat(supported("8.13")).isTrue()
        assertThat(supported("9.5")).isTrue()
        for (missing in listOf("Problems", "ProblemReporter", "ProblemSpec", "ProblemId", "ProblemGroup", "Severity")) {
            val loader = object : ClassLoader(javaClass.classLoader) {
                override fun loadClass(name: String, resolve: Boolean): Class<*> {
                    if (name == "org.gradle.api.problems.$missing") throw ClassNotFoundException(name)
                    return super.loadClass(name, resolve)
                }
            }
            assertThat(ProblemsKspDiagnosticReporterFactory.isSupported(GradleVersion.version("8.13"), loader)).isFalse()
            assertThat(KspDiagnosticReporterFactory.isSupported(loader)).isTrue()
        }
    }

    @Test
    fun factoryWrapsBtaAndRetainsItWhenProblemsAreUnavailable() {
        val project = ProjectBuilder.builder().build()
        val delegate = BtaKspDiagnosticReporter(org.gradle.api.logging.LogLevel.WARN.ordinal)
        assertThat(ProblemsKspDiagnosticReporterFactory.create(delegate, project.objects, project.logger,
            false, ":app:kspKotlin")).isInstanceOf(ProblemsKspDiagnosticReporter::class.java)
        assertThat(ProblemsKspDiagnosticReporterFactory.create(delegate, project.objects, project.logger,
            false, ":app:kspKotlin", GradleVersion.version("8.12"))).isSameInstanceAs(delegate)
        val missing = object : ClassLoader(javaClass.classLoader) {
            override fun loadClass(name: String, resolve: Boolean): Class<*> {
                if (name.startsWith("org.gradle.api.problems.")) throw ClassNotFoundException(name)
                return super.loadClass(name, resolve)
            }
        }
        assertThat(ProblemsKspDiagnosticReporterFactory.create(delegate, project.objects, project.logger,
            false, ":app:kspKotlin", GradleVersion.current(), missing)).isSameInstanceAs(delegate)
    }

    @Test
    fun consoleSuppressionRequiresGradle93WarningModeAllAndRuntimeWarnLogging() {
        for (version in listOf("8.13", "9.0", "9.2")) {
            assertThat(ProblemsKspDiagnosticReporterFactory.usesProblemsConsole(GradleVersion.version(version), true, true)).isFalse()
        }
        assertThat(ProblemsKspDiagnosticReporterFactory.usesProblemsConsole(GradleVersion.version("9.3"), true, true)).isTrue()
        assertThat(ProblemsKspDiagnosticReporterFactory.usesProblemsConsole(GradleVersion.version("9.5"), false, true)).isFalse()
        assertThat(ProblemsKspDiagnosticReporterFactory.usesProblemsConsole(GradleVersion.version("9.5"), true, false)).isFalse()
        val capture = Capture()
        val project = ProjectBuilder.builder().build()
        val logger = proxy<Logger> { name, _ -> if (name == "isWarnEnabled") false else null }
        val adapter = ProblemsKspDiagnosticReporterFactory.create(capture.delegate, project.objects, logger,
            true, ":app:kspKotlin")
        adapter.report(diagnostic(Diagnostic.Kind.ERROR))
        assertThat(capture.console.map { it.kind }).containsExactly(Diagnostic.Kind.ERROR)
    }

    @Test
    fun callbacksBufferDecoratedMessagesAndPositionsUntilWorkerFlush() {
        val capture = Capture()
        val adapter = capture.adapter()
        val message = "[ksp] Foo.kt:3: preserved body"
        val callback = Thread {
            adapter.report(diagnostic(Diagnostic.Kind.WARNING, File("/tmp/Foo.kt").toURI(), 3, 11, message))
            adapter.report(diagnostic(Diagnostic.Kind.ERROR, File("/tmp/Foo.java").toURI(), 5, 21))
            adapter.report(diagnostic(Diagnostic.Kind.ERROR))
            adapter.report(diagnostic(Diagnostic.Kind.NOTE))
        }
        callback.start()
        callback.join()
        assertThat(capture.problems).isEmpty()
        adapter.flush()
        assertThat(capture.problems.map { it["severity"] }).containsExactly(Severity.WARNING, Severity.ERROR, Severity.ERROR).inOrder()
        assertThat(capture.problems[0]["contextualLabel"]).isEqualTo(message)
        assertThat(capture.problems[0]["lineInFileLocation"]).isEqualTo(listOf(File("/tmp/Foo.kt").path, 3, 11))
        assertThat(capture.problems[1]["lineInFileLocation"]).isEqualTo(listOf(File("/tmp/Foo.java").path, 5, 21))
        assertThat(capture.problems[2]).doesNotContainKey("fileLocation")
        assertThat(capture.problems[2]).doesNotContainKey("lineInFileLocation")
        assertThat(capture.problemThreads).containsExactly(Thread.currentThread(), Thread.currentThread(), Thread.currentThread())
        assertThat(capture.console.map { it.getMessage(null) }).containsExactly(message, "[ksp] diagnostic message", "[ksp] diagnostic message")
        assertThat(capture.flushes).isEqualTo(1)
        adapter.flush()
        assertThat(capture.problems).hasSize(3)
    }

    @Test
    fun unknownColumnsAndLinesDoNotInventLocationsOrParseTheMessage() {
        val capture = Capture()
        capture.adapter().apply {
            report(diagnostic(Diagnostic.Kind.MANDATORY_WARNING, File("/tmp/Unknown.java").toURI(), 7))
            report(diagnostic(Diagnostic.Kind.WARNING, File("/tmp/File.kt").toURI()))
            report(diagnostic(Diagnostic.Kind.ERROR, message = "[ksp] /tmp/not-metadata.kt:2: message"))
            flush()
        }
        assertThat(capture.problems[0]["lineInFileLocation"]).isEqualTo(listOf(File("/tmp/Unknown.java").path, 7))
        assertThat(capture.problems[1]["fileLocation"]).isEqualTo(File("/tmp/File.kt").path)
        assertThat(capture.problems[2]).doesNotContainKey("fileLocation")
        assertThat(capture.problems[2]).doesNotContainKey("lineInFileLocation")
        assertThat(capture.problems.map { it["displayName"] }).containsExactly("KSP warning", "KSP warning", "KSP error")
    }

    @Test
    fun problemIdsAreStableAndDistinguishAllDiagnosticFieldsAndTasks() {
        val source = File("/tmp/Foo.kt").toURI()
        val base = diagnostic(Diagnostic.Kind.WARNING, source, 3, 11)
        val capture = Capture()
        val adapter = capture.adapter()
        listOf(base, base,
            diagnostic(Diagnostic.Kind.ERROR, source, 3, 11),
            diagnostic(Diagnostic.Kind.WARNING, source, 3, 11, "[ksp] another message"),
            diagnostic(Diagnostic.Kind.WARNING, File("/tmp/Other.kt").toURI(), 3, 11),
            diagnostic(Diagnostic.Kind.WARNING, source, 4, 11),
            diagnostic(Diagnostic.Kind.WARNING, source, 3, 12),
            diagnostic(Diagnostic.Kind.WARNING)).forEach(adapter::report)
        adapter.flush()
        val ids = capture.problems.map { it["id"] }
        assertThat(ids[0]).isEqualTo(ids[1])
        assertThat(ids.drop(1).toSet()).hasSize(7)
        val anotherRun = Capture()
        anotherRun.adapter().apply { report(base); flush() }
        assertThat(anotherRun.problems.single()["id"]).isEqualTo(ids[0])
        val anotherTask = Capture()
        anotherTask.adapter(taskPath = ":other:kspKotlin").apply { report(base); flush() }
        assertThat(anotherTask.problems.single()["id"]).isNotEqualTo(ids[0])
    }

    @Test
    fun problemsConsoleAvoidsDuplicatesWithoutDiscardingStructuredDiagnostics() {
        val capture = Capture()
        capture.adapter(suppressConsole = true).apply {
            report(diagnostic(Diagnostic.Kind.WARNING))
            report(diagnostic(Diagnostic.Kind.ERROR))
            flush()
        }
        assertThat(capture.problems).hasSize(2)
        assertThat(capture.console).isEmpty()
        assertThat(capture.flushes).isEqualTo(1)
    }

    private fun supported(version: String) =
        ProblemsKspDiagnosticReporterFactory.isSupported(GradleVersion.version(version), javaClass.classLoader)

    private class Capture {
        val problems = mutableListOf<Map<String, Any?>>()
        val problemThreads = mutableListOf<Thread>()
        val console = mutableListOf<Diagnostic<out JavaFileObject>>()
        var flushes = 0
        val delegate = object : KspDiagnosticReporter {
            override fun report(diagnostic: Diagnostic<out JavaFileObject>) { console.add(diagnostic) }
            override fun flush() { flushes++ }
        }
        private val reporter = proxy<ProblemReporter> { name, args ->
            if (name == "report") {
                val id = args[0] as ProblemId
                val values = mutableMapOf<String, Any?>("id" to id.name, "displayName" to id.displayName)
                val spec = proxy<ProblemSpec> { method, parameters ->
                    values[method] = if (parameters.size == 1) parameters[0] else parameters.toList()
                    null
                }
                @Suppress("UNCHECKED_CAST") (args[1] as Action<ProblemSpec>).execute(spec)
                problems.add(values)
                problemThreads.add(Thread.currentThread())
            }
            null
        }
        private val services = proxy<Problems> { _, _ -> reporter }
        fun adapter(suppressConsole: Boolean = false, taskPath: String = ":app:kspKotlin") =
            ProblemsKspDiagnosticReporter(services, delegate, suppressConsole, taskPath)
    }

    companion object {
        private inline fun <reified T> proxy(crossinline invoke: (String, Array<out Any?>) -> Any?): T =
            Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { instance, method, args ->
                val result = invoke(method.name, args ?: emptyArray())
                if (result == null && method.returnType.isInstance(instance)) instance else result
            } as T

        private fun diagnostic(
            kind: Diagnostic.Kind,
            uri: URI? = null,
            line: Long = Diagnostic.NOPOS,
            column: Long = Diagnostic.NOPOS,
            message: String = "[ksp] diagnostic message",
        ): Diagnostic<JavaFileObject> = object : Diagnostic<JavaFileObject> {
            override fun getKind() = kind
            override fun getSource(): JavaFileObject? = uri?.let { object : SimpleJavaFileObject(it, JavaFileObject.Kind.SOURCE) {} }
            override fun getLineNumber() = line
            override fun getColumnNumber() = column
            override fun getPosition() = Diagnostic.NOPOS
            override fun getStartPosition() = Diagnostic.NOPOS
            override fun getEndPosition() = Diagnostic.NOPOS
            override fun getCode(): String? = null
            override fun getMessage(locale: java.util.Locale?) = message
        }
    }
}
