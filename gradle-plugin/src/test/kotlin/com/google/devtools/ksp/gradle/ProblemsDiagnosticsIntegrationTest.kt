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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test

class ProblemsDiagnosticsIntegrationTest : DiagnosticsIntegrationTestFixture() {
    @Test
    fun warningModeAllRendersEachDecoratedWarningOnceWithStructuredLocations() {
        setup("2.4.0", "2.4.0")
        val result = problemsRunner().build()
        assertReported(result, warnings())
        assertThat(result.output).contains("ENGINE_DIAGNOSTIC_LISTENER=ProblemsKspDiagnosticReporter")
        assertThat(result.output).contains("Problem found: KSP warning")
        assertThat(result.output).doesNotContain("w: [ksp]")
        assertThat(result.task(":app:compileKotlin")!!.outcome).isEqualTo(TaskOutcome.SUCCESS)
        for (name in names) {
            val problem = kspProblems().single { it["contextualLabel"] == message("warning", name) }
            val location = locations(problem).single { it["path"] == sourceFile(name).path }
            assertThat((location["line"] as Number).toInt()).isEqualTo(2)
            assertThat((location["column"] as? Number)?.toInt() ?: -1).isAtMost(0)
            assertThat(locations(problem).any { it["taskPath"] == ":app:kspKotlin" }).isTrue()
        }
        assertThat(locations(kspProblems().single { it["contextualLabel"] == "[ksp] source-free warning" })
            .none { "path" in it }).isTrue()
    }

    @Test
    fun warningModeNoneKeepsBtaConsoleAndReportsProblems() {
        setup("2.4.0", "2.4.0")
        val result = runner().build()
        assertReported(result, warnings())
        assertThat(result.output).doesNotContain("Problem found: KSP warning")
        assertThat(result.output.lineSequence().filter { "[ksp]" in it }.toList())
            .containsExactlyElementsIn(warnings().map { "w: ${it.first}" })
    }

    @Test
    fun processingFailureFlushesErrorsAndCheckerDiagnosticsWithoutDuplicates() {
        setup("2.4.0", "2.4.0", errors = true)
        val result = problemsRunner().buildAndFail()
        assertFailure(result)
        assertReported(result, warnings() + errors() + checkerError())
        assertThat(result.output).contains("Problem found: KSP error")
        assertThat(result.output).doesNotContain("e: [ksp]")
        val checker = kspProblems().single { it["contextualLabel"] == checkerError().single().first }
        assertThat(locations(checker).single { it["path"] == sourceFile("KotlinOne").path }["line"])
            .isEqualTo(3)
    }

    @Test
    fun warningsAsErrorsFailAndRetainWarningSeverity() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("app/build.gradle.kts").appendText("\nksp { allWarningsAsErrors = true }\n")
        val result = problemsRunner().buildAndFail()
        assertFailure(result)
        assertReported(result, warnings() + checkerError())
    }

    @Test
    fun startupFailureFlushesItsSourceFreeErrorFromFinally() {
        setup("2.4.0", "2.4.0")
        tmpDir.root.resolve("processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider")
            .writeText("")
        val result = problemsRunner().buildAndFail()
        assertThat(result.task(":app:kspKotlin")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertReported(result, listOf("[ksp] No providers found in processor classpath." to "ERROR"))
        assertThat(result.output).doesNotContain("e: [ksp]")
    }

    @Test
    fun quietLoggingKeepsExplicitErrorsAndStructuredWarnings() {
        setup("2.4.0", "2.4.0", errors = true)
        val runner = problemsRunner()
        val result = runner.withArguments(runner.arguments + "--quiet").buildAndFail()
        assertFailure(result)
        assertThat(result.output).doesNotContain("warning-Kotlin")
        assertThat(result.output).doesNotContain("source-free warning")
        assertReported(result, warnings() + errors() + checkerError(), consoleMessages = errors() + checkerError())
        assertThat(result.output).contains("e: [ksp] source-free error")
    }

    @Test
    fun configurationCacheReadsCurrentWarningModeAndRuntimeQuietLogging() {
        setup("2.4.0", "2.4.0")
        val cached = problemsRunner()
        val allArguments = cached.arguments + listOf("--configuration-cache", "--configuration-cache-problems=fail", "--rerun-tasks")
        val first = cached.withArguments(allArguments).build()
        assertReported(first, warnings())
        val reused = cached.build()
        assertThat(reused.output).contains("Reusing configuration cache.")
        assertThat(reused.output).doesNotContain("FIXTURE_CONFIGURED")
        assertReported(reused, warnings())
        val noneArguments = allArguments.map { if (it == "--warning-mode=all") "--warning-mode=none" else it }
        val defaultArguments = allArguments.filterNot { it.startsWith("--warning-mode=") }
        for (arguments in listOf(noneArguments, defaultArguments)) {
            val result = cached.withArguments(arguments).build()
            assertThat(result.output).doesNotContain("FIXTURE_CONFIGURED")
            assertReported(result, warnings())
            assertThat(result.output).contains("w: [ksp] source-free warning")
        }
        write("app/src/main/java/test/ErrorTrigger.java", "package test; class ErrorTrigger {}")
        val error = "[ksp] ${tmpDir.root.resolve("app/src/main/java/test/ErrorTrigger.java").canonicalFile.invariantSeparatorsPath}:1: quiet-cache error" to "ERROR"
        val quiet = cached.withArguments(allArguments + "--quiet").buildAndFail()
        assertThat(quiet.output).doesNotContain("FIXTURE_CONFIGURED")
        assertFailure(quiet)
        // The cached legacy log level still prints warnings. Runtime WARN controls console suppression.
        assertReported(quiet, warnings() + listOf(error) + checkerError())
        assertThat(quiet.output).contains("e: ${error.first}")
        val all = cached.withArguments(allArguments).buildAndFail()
        assertThat(all.output).doesNotContain("FIXTURE_CONFIGURED")
        assertReported(all, warnings() + listOf(error) + checkerError())
        assertThat(all.output).doesNotContain("e: [ksp]")
    }

    @Test
    fun repeatedDiagnosticsKeepMoreThanFifteenDistinctLocations() {
        setup("2.4.0", "2.4.0")
        val source = sourceFile("KotlinOne")
        source.writeText("package test\n" + (1..20).joinToString("\n") { "class Many$it" })
        sourceFile("JavaOne").delete()
        val result = problemsRunner().buildAndFail()
        assertFailure(result)
        val repeated = kspProblems().filter { it["contextualLabel"].toString().endsWith(": repeated diagnostic") }
        for (severity in listOf("WARNING", "ERROR")) {
            val diagnostics = repeated.filter { it["severity"] == severity }
            assertThat(diagnostics).hasSize(20)
            assertThat(diagnostics.map { locations(it).single { it["path"] == source.path }["line"] })
                .containsExactlyElementsIn(2..21)
            val reports = result.output.split("Problem found: KSP ${severity.lowercase()}").drop(1)
            assertThat(reports.count { ": repeated diagnostic" in it }).isEqualTo(20)
        }
        assertThat(result.output.lineSequence().count { ": repeated diagnostic" in it }).isEqualTo(40)
    }

    @Test
    fun kspOptOutAlsoDisablesProblemsWithoutChangingCompilerSelection() {
        setup("2.4.0", "2.4.0")
        val runner = problemsRunner()
        val result = runner.withArguments(runner.arguments + "-Pksp.diagnostics.useBuildToolsApi=false").build()
        assertThat(result.output).contains("ENGINE_DIAGNOSTIC_LISTENER=null")
        assertThat(result.output).contains("RESOLVED_COMPILER=2.4.0")
        assertThat(result.output).contains("DIAGNOSTICS_BTA_ENABLED=true")
        assertThat(kspProblems()).isEmpty()
        assertThat(result.output.lineSequence().filter { "[ksp]" in it }.toList())
            .containsExactlyElementsIn(warnings().map { "w: ${it.first}" })
    }

    private fun problemsRunner() = runner().withArguments(":app:classes", "--warning-mode=all", "--console=plain", "--stacktrace")

    private fun assertFailure(result: BuildResult) {
        assertThat(result.task(":app:kspKotlin")!!.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains("KSP failed with exit code: PROCESSING_ERROR")
    }

    private fun assertReported(
        result: BuildResult,
        expected: List<Pair<String, String>>,
        consoleMessages: List<Pair<String, String>> = expected,
    ) {
        assertThat(kspProblems().map { it["contextualLabel"] to it["severity"] }).containsExactlyElementsIn(expected)
        for ((message, _) in consoleMessages) {
            assertThat(result.output.lineSequence().count { message in it }).isEqualTo(1)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun locations(problem: Map<String, Any?>) = problem["locations"] as List<Map<String, Any?>>

    private fun message(kind: String, name: String) = "[ksp] ${sourceFile(name).invariantSeparatorsPath}:2: $kind-$name"
    private fun warnings() = names.map { message("warning", it) to "WARNING" } + ("[ksp] source-free warning" to "WARNING")
    private fun errors() = names.map { message("error", it) to "ERROR" } + ("[ksp] source-free error" to "ERROR")
    private fun checkerError() = listOf("[ksp] Symbols.kt:3: Unresolved reference 'Generated'." to "ERROR")
}
