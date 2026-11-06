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

import com.google.devtools.ksp.IncrementalContextLoggingOptions
import com.google.devtools.ksp.common.FileToFilesMap
import com.google.devtools.ksp.common.FileToSymbolsMap
import com.google.devtools.ksp.common.LookupSymbolWrapper
import com.google.devtools.ksp.common.NoSourceFile
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.DualLookupTracker
import com.google.devtools.ksp.impl.IncrementalContextAA
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.FileSystemException

/**
 * Tests for [IncrementalContextAA.tryEarlyExitIfClean]: KSP skips the session and the processors when an incremental
 * run has nothing dirty.
 *
 * Most tests run one full incremental build with [build], then ask [canExitEarly] whether the next run may be skipped.
 */
class IncrementalEarlyExitTest {
    @TempDir
    lateinit var tmp: File

    private val base by lazy { File(tmp, "project") }
    private val caches by lazy { File(tmp, "caches") }
    private val out by lazy { File(tmp, "out") }
    private val upToDate by lazy { File(caches, "caches.uptodate") }

    private val a by lazy { src("p1/A.kt") }
    private val aGen by lazy { gen("p1/AGen.kt") }

    @Test
    fun `exits early and restores all outputs when the classpath change is unrelated`() {
        val aGen2 = gen("p1/AGen2.kt") // Generated in round 2, from AGen.kt.
        build(mapOf(a to setOf(aGen), aGen to setOf(aGen2)))
        out.deleteRecursively() // As the Gradle plugin does before each run.
        val cacheFiles = listOf("symbols", "sealed", "sourceToOutputs").map { File(caches, it) }
        cacheFiles.forEach { it.setLastModified(10_000) }

        assertTrue(canExitEarly(changed = listOf("p1.Unrelated")))

        assertEquals("gen p1/AGen.kt", aGen.readText())
        assertEquals("gen p1/AGen2.kt", aGen2.readText())
        cacheFiles.forEach { assertEquals(10_000, it.lastModified(), "$it must not be rewritten") }
    }

    @Test
    fun `restores outputs that were edited since the last run, like the full path`() {
        build(mapOf(a to setOf(aGen)))
        aGen.writeText("edited")

        assertTrue(canExitEarly())
        assertEquals("gen p1/AGen.kt", aGen.readText())
    }

    @Test
    fun `no early exit when not incremental, on the first build, or when sources changed`() {
        assertFalse(canExitEarly(), "first build: there are no caches yet")
        build(mapOf(a to setOf(aGen)))

        assertFalse(canExitEarly(incremental = false))
        assertFalse(canExitEarly(modified = listOf(a)))
        assertFalse(canExitEarly(removed = listOf(a)))
    }

    @Test
    fun `no early exit when a changed class was looked up by a source`() {
        val j = src("p1/J.java")
        build(mapOf(a to setOf(aGen)), sources = listOf(a, j)) { tracker ->
            tracker.classTracker.record(a.path, Position.NO_POSITION, "p1", ScopeKind.PACKAGE, "UsedClass")
            tracker.symbolTracker.record(j.path, Position.NO_POSITION, "p1", ScopeKind.PACKAGE, "UsedSymbol")
        }

        assertFalse(canExitEarly(changed = listOf("p1.UsedClass")), "class lookup from a Kotlin file")
        assertFalse(canExitEarly(changed = listOf("p1.UsedSymbol")), "symbol lookup, as recorded for Java files")
        assertTrue(canExitEarly(changed = listOf("p1.Unrelated")))
    }

    @Test
    fun `no early exit when a changed class has a NoSourceFile output`() {
        build(mapOf(File(NoSourceFile(base, "p1.L4").filePath) to setOf(aGen)))

        assertFalse(canExitEarly(changed = listOf("p1.L4")))
        assertTrue(canExitEarly(changed = listOf("p1.Unrelated")))
    }

    @Test
    fun `no early exit when the module has an aggregating output`() {
        build(mapOf(File(base, ANY_CHANGES) to setOf(aGen)))
        assertFalse(canExitEarly())
    }

    @Test
    fun `no early exit when the module has an output without sources`() {
        build(sourceToOutputs = emptyMap(), outputs = setOf(aGen)) // Dependencies(false)
        assertFalse(canExitEarly())
    }

    @Test
    fun `no early exit when the caches predate tracking of outputs without sources`() {
        build(mapOf(a to setOf(aGen)))
        FileToFilesMap(File(caches, "sourceToOutputs")).apply {
            val key = keys.single { it.path.startsWith("<This is a virtual key for unassociated outputs") }
            assertEquals(emptyList<File>(), remove(key), "the key is written even when there are no such outputs")
            flush()
        }

        assertFalse(canExitEarly())
    }

    @Test
    fun `no early exit when a source asked for sealed subclasses`() {
        build(mapOf(a to setOf(aGen)))
        FileToSymbolsMap(File(caches, "sealed")).apply {
            put(a.relativeTo(base), listOf(LookupSymbolWrapper("A", "p1")))
            flush()
        }

        assertFalse(canExitEarly())
    }

    @Test
    fun `no early exit when an output disappeared in the last run`() {
        build(mapOf(a to setOf(aGen)))
        build(sourceToOutputs = emptyMap(), modified = listOf(a)) // A.kt no longer generates AGen.kt.

        assertFalse(canExitEarly())
    }

    @Test
    fun `an output that cannot be restored leads to a full rebuild, not the same failure every run`() {
        build(mapOf(a to setOf(aGen)))
        File(caches, "backups").deleteRecursively()
        out.deleteRecursively()

        assertFalse(canExitEarly())
        assertTrue(upToDate.exists(), "declining the early exit must not touch the caches")
        // The full path then fails as without the early exit, but only after invalidating the caches.
        context().run {
            val dirty = calcDirtyFiles(listOf(FakeKSFile(a)))
            assertThrows(FileSystemException::class.java) { updateCachesAndOutputs(dirty, emptySet(), emptyMap()) }
        }
        assertFalse(upToDate.exists())
    }

    @Test
    fun `a failure while restoring outputs invalidates the caches`() {
        build(mapOf(a to setOf(aGen)))
        out.deleteRecursively()
        File(out, "kotlin/p1").apply { parentFile.mkdirs() }.writeText("a file where a directory should be")

        assertThrows(FileSystemException::class.java) { canExitEarly() }
        assertFalse(upToDate.exists())
    }

    @Test
    fun `KSP skips the processors on a clean run`() {
        val processor = processor { resolver, codeGenerator ->
            val files = resolver.getAllFiles().toList().toTypedArray()
            codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
        }
        runKsp(processor)
        val generated = File(out, "kotlin/p1/AGen.kt")
        out.deleteRecursively()

        val failing = SymbolProcessorProvider { error("processors must not be created on a clean run") }
        runKsp(failing, changed = listOf("other.Unrelated"), log = true)

        assertEquals("gen", generated.readText())
        val log = File(caches, "logs/kspDirtySet.log").readText()
        val aPath = a.relativeTo(base).path // Uses the platform's separator, like the log.
        assertEquals(1, log.lines().count { it.trim() == aPath }, "A.kt is listed once, though source roots overlap")
        assertTrue("Dirty / All: 0.00%" in log)
    }

    @Test
    fun `KSP runs the processors again when there are outputs without sources`() {
        var created = 0
        val processor = processor { _, codeGenerator ->
            codeGenerator.createNewFile(Dependencies(false), "p1", "Gen").use { it.write("gen".toByteArray()) }
        }
        val counting = SymbolProcessorProvider { created++; processor.create(it) }
        runKsp(counting)
        out.deleteRecursively()

        runKsp(counting, changed = listOf("other.Unrelated"))

        assertEquals(2, created)
        assertEquals("gen", File(out, "kotlin/p1/Gen.kt").readText())
    }

    private fun src(path: String) = File(base, "src/main/kotlin/$path").apply {
        parentFile.mkdirs()
        writeText(if (extension == "java") "package p1; class $nameWithoutExtension {}" else "package p1")
    }

    private fun gen(path: String) = File(out, "kotlin/$path").apply {
        parentFile.mkdirs()
        writeText("gen $path")
    }

    private fun context(
        modified: List<File> = emptyList(),
        removed: List<File> = emptyList(),
        changed: List<String> = emptyList(),
        incremental: Boolean = true,
        tracker: DualLookupTracker = DualLookupTracker(),
    ) = IncrementalContextAA(
        isIncremental = incremental,
        lookupTracker = tracker,
        anyChangesWildcard = File(ANY_CHANGES),
        loggingOptions = IncrementalContextLoggingOptions(false, null),
        baseDir = base,
        cachesDir = caches,
        kspOutputDir = out,
        knownModified = modified,
        knownRemoved = removed,
        changedClasses = changed,
    )

    /** Runs a full incremental build in which [outputs] were generated from the sources in [sourceToOutputs]. */
    private fun build(
        sourceToOutputs: Map<File, Set<File>>,
        outputs: Set<File> = sourceToOutputs.values.flatten().toSet(),
        modified: List<File> = emptyList(),
        sources: List<File> = listOf(a),
        recordLookups: (DualLookupTracker) -> Unit = {},
    ) {
        val tracker = DualLookupTracker()
        context(modified = modified, tracker = tracker).run {
            val dirty = calcDirtyFiles(sources.map(::FakeKSFile))
            recordLookups(tracker)
            val relative = sourceToOutputs.entries.associate { (src, outs) ->
                src.relativeTo(base) to outs.map { it.relativeTo(base) }.toSet()
            }
            updateCachesAndOutputs(dirty, outputs, relative)
            closeFiles()
        }
    }

    private fun canExitEarly(
        modified: List<File> = emptyList(),
        removed: List<File> = emptyList(),
        changed: List<String> = emptyList(),
        incremental: Boolean = true,
    ): Boolean {
        val context = context(modified, removed, changed, incremental)
        return context.tryEarlyExitIfClean { emptyList() }.also { exited ->
            if (!exited) context.closeFiles()
        }
    }

    private fun processor(generate: (Resolver, CodeGenerator) -> Unit) =
        SymbolProcessorProvider { env ->
            object : SymbolProcessor {
                var done = false
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (!done) generate(resolver, env.codeGenerator)
                    done = true
                    return emptyList()
                }
            }
        }

    private fun runKsp(provider: SymbolProcessorProvider, changed: List<String> = emptyList(), log: Boolean = false) {
        val srcRoot = a.parentFile.parentFile
        val config = KSPJvmConfig.Builder().apply {
            moduleName = "test"
            sourceRoots = listOf(srcRoot)
            javaSourceRoots = listOf(srcRoot) // Overlaps with sourceRoots, as in a typical Gradle setup.
            commonSourceRoots = emptyList()
            libraries = emptyList()
            friends = emptyList()
            jdkHome = JDK_HOME
            jvmTarget = "17"
            languageVersion = "2.0"
            apiVersion = "2.0"
            projectBaseDir = base
            outputBaseDir = out
            classOutputDir = File(out, "classes")
            javaOutputDir = File(out, "java")
            kotlinOutputDir = File(out, "kotlin")
            resourceOutputDir = File(out, "resources")
            cachesDir = caches
            incremental = true
            incrementalLog = log
            modifiedSources = emptyList()
            removedSources = emptyList()
            changedClasses = changed
        }.build()
        val exitCode = KotlinSymbolProcessing(config, listOf(provider), CommandLineKSPLogger()).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exitCode)
    }

    private class FakeKSFile(file: File) : KSFile {
        override val filePath = file.path
        override val fileName = file.name
        override val packageName = KSNameImpl.getCached("p1")
        override val declarations = emptySequence<Nothing>()
        override val annotations = emptySequence<Nothing>()
        override val origin = Origin.KOTLIN
        override val location = FileLocation(filePath, 1)
        override val parent = null
        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = visitor.visitFile(this, data)
    }

    private companion object {
        const val ANY_CHANGES = "<any>"
        @JvmStatic
        val JDK_HOME = File(System.getProperty("java.home"))
    }
}
