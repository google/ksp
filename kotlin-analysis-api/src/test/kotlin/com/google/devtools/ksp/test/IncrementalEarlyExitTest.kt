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
import com.google.devtools.ksp.common.ClassQueries
import com.google.devtools.ksp.common.FileToFilesMap
import com.google.devtools.ksp.common.FileToSymbolsMap
import com.google.devtools.ksp.common.LookupSymbolWrapper
import com.google.devtools.ksp.common.NoSourceFile
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.DualLookupTracker
import com.google.devtools.ksp.impl.IncrementalContextAA
import com.google.devtools.ksp.impl.KSPLoader
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.KspGradleLogger.Companion.LOGGING_LEVEL_WARN
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
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectOutputStream
import java.nio.file.FileSystemException
import java.util.function.Supplier
import javax.tools.ToolProvider

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
    private val aGenBackup by lazy { File(caches, "backups/kotlin/p1/AGen.kt") }

    @Test
    fun `exits early and restores all outputs when the classpath change is unrelated`() {
        val aGen2 = gen("p1/AGen2.kt") // Generated in round 2, from AGen.kt.
        build(mapOf(a to setOf(aGen), aGen to setOf(aGen2)))
        out.deleteRecursively() // As callers that clean the outputs before each run do.
        val cacheFiles = listOf("symbols", "sealed", "sourceToOutputs", "classQueries", "packageQueries").map {
            File(caches, it)
        }
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
    fun `does not copy outputs that still match their backups`() {
        build(mapOf(a to setOf(aGen)))
        // Same size and timestamp as the backup, so the early exit takes it to be unchanged.
        aGen.writeText("GEN p1/AGen.kt")
        aGen.setLastModified(aGenBackup.lastModified())

        assertTrue(canExitEarly())
        assertEquals("GEN p1/AGen.kt", aGen.readText())
    }

    @Test
    fun `restores outputs with the same size as their backups but a different timestamp`() {
        build(mapOf(a to setOf(aGen)))
        aGen.writeText("GEN p1/AGen.kt")
        aGen.setLastModified(aGenBackup.lastModified() + 2_000)

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
            tracker.classTracker.record(a.path, Position.NO_POSITION, "p1.Outer", ScopeKind.CLASSIFIER, "Inner")
            tracker.classTracker.record(a.path, Position.NO_POSITION, "p1", ScopeKind.PACKAGE, "Top\$Gen")
            tracker.classTracker.record(a.path, Position.NO_POSITION, "pkg\$1", ScopeKind.PACKAGE, "InDollarPkg")
            tracker.classTracker.record(a.path, Position.NO_POSITION, "pkg\$1.Outer", ScopeKind.CLASSIFIER, "Inner")
            tracker.classTracker.record(a.path, Position.NO_POSITION, "p1.\$Outer", ScopeKind.CLASSIFIER, "Inner")
            tracker.classTracker.record(a.path, Position.NO_POSITION, "p1.Outer\$Gen", ScopeKind.CLASSIFIER, "Inner")
            tracker.symbolTracker.record(j.path, Position.NO_POSITION, "p1", ScopeKind.PACKAGE, "UsedSymbol")
        }

        assertFalse(canExitEarly(changed = listOf("p1.UsedClass")), "class lookup from a Kotlin file")
        assertFalse(canExitEarly(changed = listOf("p1.Outer\$Inner")), "nested class lookup with JVM '$' separator")
        assertFalse(canExitEarly(changed = listOf("p1.Top\$Gen")), "top-level '$'-named class lookup")
        assertFalse(canExitEarly(changed = listOf("pkg\$1.InDollarPkg")), "class in '$'-named package")
        assertFalse(canExitEarly(changed = listOf("pkg\$1.Outer\$Inner")), "nested class in '$'-named package")
        assertFalse(canExitEarly(changed = listOf("p1.\$Outer\$Inner")), "nested class inside leading-'$' outer class")
        assertFalse(canExitEarly(changed = listOf("p1.Outer\$Gen\$Inner")), "nested class inside '$'-named outer class")
        assertFalse(canExitEarly(changed = listOf("p1.UsedSymbol")), "symbol lookup, as recorded for Java files")
        assertTrue(canExitEarly(changed = listOf("p1.Unrelated")))
    }

    @Test
    fun `no early exit when a changed class has a NoSourceFile output`() {
        val bGen = gen("p1/BGen.kt")
        build(
            mapOf(
                File(NoSourceFile(base, "p1.L4").filePath) to setOf(aGen),
                File(NoSourceFile(base, "p1.MyEnum.ENTRY").filePath) to setOf(bGen),
            ),
        )

        assertFalse(canExitEarly(changed = listOf("p1.L4")), "exact NoSourceFile match")
        assertFalse(canExitEarly(changed = listOf("p1.L4.Nested")), "nested class change under associated outer class")
        assertFalse(canExitEarly(changed = listOf("p1.MyEnum")), "enclosing class change of associated enum entry")
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
    fun `no early exit when the caches predate tracking of API queries`() {
        build(mapOf(a to setOf(aGen)))
        assertTrue(File(caches, "classQueries").delete())
        assertFalse(canExitEarly(), "missing classQueries cache must decline early exit")

        build(mapOf(a to setOf(aGen)))
        assertTrue(File(caches, "packageQueries").delete())
        assertFalse(canExitEarly(), "missing packageQueries cache must decline early exit")
    }

    @Test
    fun `missing query caches trigger a full rebuild so clean sources populate queries`() {
        val b = src("p1/B.kt")
        val perFileProcessor = processor { resolver, codeGenerator ->
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            for (file in resolver.getAllFiles()) {
                when (file.fileName) {
                    "A.kt" -> {
                        resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.FromA"))
                        resolver.getDeclarationsFromPackage("pkg_from_a").toList()
                        codeGenerator.createNewFile(Dependencies(false, file), "p1", "AGen")
                            .use { it.write("gen A".toByteArray()) }
                    }
                    "B.kt" -> {
                        resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.FromB"))
                        resolver.getDeclarationsFromPackage("pkg_from_b").toList()
                        codeGenerator.createNewFile(Dependencies(false, file), "p1", "BGen")
                            .use { it.write("gen B".toByteArray()) }
                    }
                }
            }
        }
        runKsp(perFileProcessor)

        // Simulate caches written by an older KSP (caches.uptodate exists, classQueries missing).
        assertTrue(File(caches, "classQueries").delete())
        out.deleteRecursively()
        runKsp(perFileProcessor)
        assertFalse(
            canExitEarly(changed = listOf("lib.FromB")),
            "rebuild after missing classQueries must reprocess clean B.kt and record its class queries",
        )

        // Simulate missing packageQueries as well.
        assertTrue(File(caches, "packageQueries").delete())
        out.deleteRecursively()
        runKsp(perFileProcessor)
        assertFalse(
            canExitEarly(changed = listOf("pkg_from_b.Added")),
            "rebuild after missing packageQueries must reprocess clean B.kt and record its package queries",
        )
        assertTrue(b.exists())
    }

    @Test
    fun `cache-upgrade rebuild prunes disappeared outputs from lookup caches`() {
        val aGen2 = gen("p1/AGen2.kt")
        val deletedSrc = src("p1/DeletedBeforeRebuild.kt")
        build(mapOf(a to setOf(aGen), aGen to setOf(aGen2)), sources = listOf(a, deletedSrc)) { tracker ->
            tracker.classTracker.record(aGen.path, Position.NO_POSITION, "p1", ScopeKind.PACKAGE, "FromOldOutput")
            tracker.symbolTracker.record(
                aGen2.path,
                Position.NO_POSITION,
                "p1",
                ScopeKind.PACKAGE,
                "FromPendingRemovedOutput",
            )
            tracker.symbolTracker.record(
                NoSourceFile(base, "lib.OldBinary").filePath,
                Position.NO_POSITION,
                "p1",
                ScopeKind.PACKAGE,
                "FromOldNoSource",
            )
            tracker.classTracker.record(
                deletedSrc.path,
                Position.NO_POSITION,
                "p1",
                ScopeKind.PACKAGE,
                "FromUntrackedDeletedSource",
            )
        }
        // Pre-upgrade incremental build drops aGen2 so it sits in removedOutputsKey.
        build(mapOf(a to setOf(aGen)), modified = listOf(a), sources = listOf(a, deletedSrc))

        // Simulate cache upgrade (missing classQueries -> rebuild) where aGen is gone and deletedSrc was removed
        // without appearing in knownRemoved.
        assertTrue(deletedSrc.delete())
        assertTrue(File(caches, "classQueries").delete())
        build(sourceToOutputs = emptyMap(), sources = listOf(a))

        assertTrue(
            canExitEarly(changed = listOf("p1.FromOldOutput")),
            "rebuild must prune disappeared outputs from classLookupCache",
        )
        assertTrue(
            canExitEarly(changed = listOf("p1.FromPendingRemovedOutput")),
            "rebuild must prune pending removedOutputsKey entries from symbolLookupCache",
        )
        assertTrue(
            canExitEarly(changed = listOf("p1.FromOldNoSource")),
            "rebuild must clear stale NoSourceFile entries from symbolLookupCache",
        )
        assertTrue(
            canExitEarly(changed = listOf("p1.FromUntrackedDeletedSource")),
            "rebuild must clear untracked deleted-source entries from classLookupCache",
        )
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
        runKsp(generatesAGen)
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

    @Test
    fun `KSP loads the processors only when it processes`() {
        var loads = 0
        val loading = Supplier { loads++; listOf(generatesAGen) }

        runKsp(loading)
        runKsp(loading, changed = listOf("other.Unrelated"))

        assertEquals(1, loads, "the clean second run must not load the processors")
    }

    @Test
    fun `an exception from loading the processors propagates and closes the caches`() {
        val failure = IllegalStateException("cannot load processors")

        // With the incremental log, the caches directory holds open log files until the incremental context is closed.
        val thrown = assertThrows(IllegalStateException::class.java) { runKsp(Supplier { throw failure }, log = true) }

        assertSame(failure, thrown)
        assertEquals(emptyList<String>(), openFilesIn(caches))
    }

    @Test
    fun `KSPLoader loads the processors only when KSP processes`() {
        var loads = 0
        val loading = Supplier { loads++; listOf(generatesAGen) }

        assertEquals(0, KSPLoader.loadAndRunKSP(serialized(config()), loading, LOGGING_LEVEL_WARN))
        val unrelatedChange = serialized(config(changed = listOf("other.Unrelated")))
        assertEquals(0, KSPLoader.loadAndRunKSP(unrelatedChange, loading, LOGGING_LEVEL_WARN))

        assertEquals(1, loads, "the clean second run must not load the processors")
    }

    @Test
    fun `KSPLoader runs a list of processors`() {
        assertEquals(0, KSPLoader.loadAndRunKSP(serialized(config()), listOf(generatesAGen), LOGGING_LEVEL_WARN))
        assertEquals("gen", File(out, "kotlin/p1/AGen.kt").readText())
    }

    @Test
    fun `KSP runs the processors when a class queried by getClassDeclarationByName changes`() {
        val initial = processor { resolver, codeGenerator ->
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.OptionalFeature"))
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.MyEnum.ENTRY"))
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.Outer"))
            val files = resolver.getAllFiles().toList().toTypedArray()
            codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
        }
        runKsp(initial)

        // Unrelated classes (including sibling nested/enum names) still take the early exit.
        out.deleteRecursively()
        val failing = SymbolProcessorProvider { error("processors must not be created on an unrelated change") }
        runKsp(failing, changed = listOf("lib.Unrelated", "lib.MyEnum.OTHER_ENTRY", "other.Foo"))
        assertEquals("gen", File(out, "kotlin/p1/AGen.kt").readText())

        // Exact match, enclosing class of a queried nested/enum entry, and nested class of a queried outer class
        // must all decline early exit and run the processors.
        for (changedClass in listOf("lib.OptionalFeature", "lib.MyEnum", "lib.Outer.Inner")) {
            var runs = 0
            val counting = SymbolProcessorProvider { runs++; initial.create(it) }
            out.deleteRecursively()
            runKsp(counting, changed = listOf(changedClass))
            assertEquals(1, runs, "expected processor to run when $changedClass changed")
            assertEquals("gen", File(out, "kotlin/p1/AGen.kt").readText())
        }
    }

    @Test
    fun `classQueries unions across incremental runs and resets on rebuild`() {
        val b = src("p1/B.kt")
        a // Ensure A.kt exists before the initial build.
        val queryPerDirtyFile = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                when (file.fileName) {
                    "A.kt" -> resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.FromA"))
                    "B.kt" -> resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.FromB"))
                }
                codeGenerator.createNewFile(Dependencies(false, file), "p1", "${file.fileName}Gen").use {
                    it.write("gen".toByteArray())
                }
            }
        }
        // Run 1 (full build): processes both A.kt and B.kt.
        runKsp(queryPerDirtyFile)

        // Run 2 (incremental): only A.kt is modified, so B.kt is clean and "lib.FromB" is not queried in Run 2.
        a.writeText("package p1\nclass AModified")
        out.deleteRecursively()
        runKsp(queryPerDirtyFile, modified = listOf(a))

        assertFalse(canExitEarly(changed = listOf("lib.FromB")), "query from clean B.kt must be retained across runs")
        assertFalse(canExitEarly(changed = listOf("lib.FromA")), "query from modified A.kt must be recorded")
        assertTrue(canExitEarly(changed = listOf("lib.Unrelated")))

        // Rebuild (caches.uptodate deleted) where no class queries are made must overwrite old queries.
        assertTrue(upToDate.delete())
        val noQueries = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                codeGenerator.createNewFile(Dependencies(false, file), "p1", "${file.fileName}Gen").use {
                    it.write("gen".toByteArray())
                }
            }
        }
        out.deleteRecursively()
        runKsp(noQueries)

        assertTrue(canExitEarly(changed = listOf("lib.FromB")), "rebuild must clear stale classQueries")
        assertTrue(canExitEarly(changed = listOf("lib.FromA")), "rebuild must clear stale classQueries")
        assertTrue(b.exists())
    }

    @Test
    fun `KSP runs the processors when a package queried by getDeclarationsFromPackage changes, ignoring subpackages`() {
        val libClassesDir = compileJavaLibrary(
            "hints/Outer.java" to "package hints; public class Outer { public static class Inner {} }",
        )
        val initial = processor { resolver, codeGenerator ->
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            resolver.getDeclarationsFromPackage("hints").toList()
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            resolver.getDeclarationsFromPackage("empty_pkg").toList()
            val files = resolver.getAllFiles().toList().toTypedArray()
            codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
        }
        runKsp(initial, libraries = listOf(libClassesDir))

        // Subpackages ("hints.sub.*", "empty_pkg.sub.*"), root package, and unrelated packages still exit early.
        out.deleteRecursively()
        val failing = SymbolProcessorProvider { error("processors must not run on subpackage or unrelated changes") }
        runKsp(
            failing,
            changed = listOf("hints.sub.Bar", "empty_pkg.sub.Baz", "RootClass", "other.Unrelated"),
            libraries = listOf(libClassesDir),
        )
        assertEquals("gen", File(out, "kotlin/p1/AGen.kt").readText())

        // Top-level changes in queried packages AND nested class changes inside yielded library classes must run.
        for (changedClass in listOf("empty_pkg.NewHint", "hints.AnotherHint", "hints.Outer.Inner")) {
            var runs = 0
            val counting = SymbolProcessorProvider { runs++; initial.create(it) }
            out.deleteRecursively()
            runKsp(counting, changed = listOf(changedClass), libraries = listOf(libClassesDir))
            assertEquals(1, runs, "expected processor to run when $changedClass changed")
        }
    }

    @Test
    fun `getDeclarationsFromPackage on root package only matches root-package classes`() {
        val initial = processor { resolver, codeGenerator ->
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            resolver.getDeclarationsFromPackage("").toList()
            val files = resolver.getAllFiles().toList().toTypedArray()
            codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
        }
        runKsp(initial)

        out.deleteRecursively()
        val failing = SymbolProcessorProvider { error("processors must not run when named-package classes change") }
        runKsp(failing, changed = listOf("p1.Foo", "com.example.Bar"))
        assertEquals("gen", File(out, "kotlin/p1/AGen.kt").readText())

        var runs = 0
        val counting = SymbolProcessorProvider { runs++; initial.create(it) }
        out.deleteRecursively()
        runKsp(counting, changed = listOf("NewRootClass"))
        assertEquals(1, runs, "expected processor to run when a root-package class changed")
    }

    @Test
    fun `KSP records getFunctionDeclarationsByName for member and top-level lookups`() {
        val initial = processor { resolver, codeGenerator ->
            resolver.getFunctionDeclarationsByName(
                resolver.getKSNameFromString("member_pkg.Owner.myFunc"),
                includeTopLevel = false,
            ).toList()
            resolver.getFunctionDeclarationsByName(
                resolver.getKSNameFromString("top_fn_pkg.myTopFunc"),
                includeTopLevel = true,
            ).toList()
            resolver.getFunctionDeclarationsByName(
                resolver.getKSNameFromString("missing_cls_pkg.MissingOwner.myFunc"),
                includeTopLevel = true,
            ).toList()
            val files = resolver.getAllFiles().toList().toTypedArray()
            codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
        }
        runKsp(initial)

        // Unrelated classes in member_pkg, subpackages of top_fn_pkg, and unrelated packages still exit early.
        out.deleteRecursively()
        val failing = SymbolProcessorProvider { error("processors must not run on unrelated changes") }
        runKsp(
            failing,
            changed = listOf(
                "member_pkg.OtherClass",
                "member_pkg.TopFileKt",
                "top_fn_pkg.sub.Unrelated",
                "missing_cls_pkg.OtherClass",
                "other.Unrelated",
            ),
        )
        assertEquals("gen", File(out, "kotlin/p1/AGen.kt").readText())

        // Owner class change (includeTopLevel = false), facade/class change in top_fn_pkg (includeTopLevel = true),
        // and newly added MissingOwner class (includeTopLevel = true) must all run processors.
        for (changedClass in listOf("member_pkg.Owner", "top_fn_pkg.NewFunctionsKt", "missing_cls_pkg.MissingOwner")) {
            var runs = 0
            val counting = SymbolProcessorProvider { runs++; initial.create(it) }
            out.deleteRecursively()
            runKsp(counting, changed = listOf(changedClass))
            assertEquals(1, runs, "expected processor to run when $changedClass changed")
        }
    }

    @Test
    fun `KSP records getPropertyDeclarationByName for member and top-level lookups`() {
        val initial = processor { resolver, codeGenerator ->
            resolver.getPropertyDeclarationByName(
                resolver.getKSNameFromString("member_pkg.Owner.myProp"),
                includeTopLevel = false,
            )
            resolver.getPropertyDeclarationByName(
                resolver.getKSNameFromString("top_prop_pkg.myTopProp"),
                includeTopLevel = true,
            )
            resolver.getPropertyDeclarationByName(
                resolver.getKSNameFromString("missing_cls_pkg.MissingOwner.myProp"),
                includeTopLevel = true,
            )
            val files = resolver.getAllFiles().toList().toTypedArray()
            codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
        }
        runKsp(initial)

        out.deleteRecursively()
        val failing = SymbolProcessorProvider { error("processors must not run on unrelated changes") }
        runKsp(
            failing,
            changed = listOf(
                "member_pkg.OtherClass",
                "member_pkg.TopFileKt",
                "top_prop_pkg.sub.Unrelated",
                "missing_cls_pkg.OtherClass",
                "other.Unrelated",
            ),
        )
        assertEquals("gen", File(out, "kotlin/p1/AGen.kt").readText())

        for (changedClass in listOf("member_pkg.Owner", "top_prop_pkg.NewPropsKt", "missing_cls_pkg.MissingOwner")) {
            var runs = 0
            val counting = SymbolProcessorProvider { runs++; initial.create(it) }
            out.deleteRecursively()
            runKsp(counting, changed = listOf(changedClass))
            assertEquals(1, runs, "expected processor to run when $changedClass changed")
        }
    }

    @Test
    fun `packageQueries unions across incremental runs and resets on rebuild`() {
        val b = src("p1/B.kt")
        a // Ensure A.kt exists before the initial build.
        val queryPerDirtyFile = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                @OptIn(com.google.devtools.ksp.KspExperimental::class)
                when (file.fileName) {
                    "A.kt" -> resolver.getDeclarationsFromPackage("pkg_from_a").toList()
                    "B.kt" -> resolver.getDeclarationsFromPackage("pkg_from_b").toList()
                }
                codeGenerator.createNewFile(Dependencies(false, file), "p1", "${file.fileName}Gen").use {
                    it.write("gen".toByteArray())
                }
            }
        }
        runKsp(queryPerDirtyFile)

        // Incremental run modifying only A.kt must retain pkg_from_b from clean B.kt.
        a.writeText("package p1\nclass AModified")
        out.deleteRecursively()
        runKsp(queryPerDirtyFile, modified = listOf(a))

        assertFalse(canExitEarly(changed = listOf("pkg_from_b.Added")), "package query from clean B.kt must remain")
        assertFalse(canExitEarly(changed = listOf("pkg_from_a.Added")), "package query from modified A.kt must remain")
        assertTrue(canExitEarly(changed = listOf("pkg_from_b.sub.Ignored")))

        // Rebuild clears stale packageQueries.
        assertTrue(upToDate.delete())
        val noQueries = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                codeGenerator.createNewFile(Dependencies(false, file), "p1", "${file.fileName}Gen").use {
                    it.write("gen".toByteArray())
                }
            }
        }
        out.deleteRecursively()
        runKsp(noQueries)

        assertTrue(canExitEarly(changed = listOf("pkg_from_b.Added")), "rebuild must clear stale packageQueries")
        assertTrue(canExitEarly(changed = listOf("pkg_from_a.Added")), "rebuild must clear stale packageQueries")
        assertTrue(b.exists())
    }

    @Test
    fun `processor generates new output when a declaration in a queried package appears on the classpath`() {
        val featureAwareProcessor = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                codeGenerator.createNewFile(Dependencies(false, file), "p1", "AGen").use {
                    it.write("gen A".toByteArray())
                }
            }
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            val features = resolver.getDeclarationsFromPackage("hints")
                .filterIsInstance<com.google.devtools.ksp.symbol.KSClassDeclaration>()
                .toList()
            if (features.isNotEmpty()) {
                codeGenerator.createNewFile(Dependencies(false), "p1", "FeatureConfig").use {
                    val names = features.joinToString(",") { cls -> cls.simpleName.asString() }
                    it.write("features=$names".toByteArray())
                }
                codeGenerator.associateWithClasses(features, "p1", "FeatureConfig")
            }
        }

        // Run 1: "hints" package does not exist yet; only AGen.kt is generated.
        runKsp(featureAwareProcessor)
        assertEquals("gen A", File(out, "kotlin/p1/AGen.kt").readText())
        assertFalse(File(out, "kotlin/p1/FeatureConfig.kt").exists())

        // Run 2: upstream library adds hints.Feature (no module sources modified).
        val libDir = compileJavaLibrary("hints/Feature.java" to "package hints; public class Feature {}")
        out.deleteRecursively()
        runKsp(featureAwareProcessor, changed = listOf("hints.Feature"), libraries = listOf(libDir))
        assertEquals("gen A", File(out, "kotlin/p1/AGen.kt").readText())
        assertEquals("features=Feature", File(out, "kotlin/p1/FeatureConfig.kt").readText())

        // Run 3: unrelated classpath change exits early and restores both AGen.kt and FeatureConfig.kt.
        out.deleteRecursively()
        val failing = SymbolProcessorProvider { error("processors must not run on an unrelated change") }
        runKsp(failing, changed = listOf("other.Unrelated"), libraries = listOf(libDir))
        assertEquals("gen A", File(out, "kotlin/p1/AGen.kt").readText())
        assertEquals("features=Feature", File(out, "kotlin/p1/FeatureConfig.kt").readText())

        // Run 4: upstream library removes hints.Feature; FeatureConfig.kt must disappear and not be restored.
        val emptyLibDir = compileJavaLibrary()
        out.deleteRecursively()
        runKsp(featureAwareProcessor, changed = listOf("hints.Feature"), libraries = listOf(emptyLibDir))
        assertEquals("gen A", File(out, "kotlin/p1/AGen.kt").readText())
        assertFalse(
            File(out, "kotlin/p1/FeatureConfig.kt").exists(),
            "stale FeatureConfig.kt must not be restored from backup after hints.Feature is removed",
        )

        // Run 5: processes disappeared output invalidation (removedOutputsKey), still without FeatureConfig.kt.
        out.deleteRecursively()
        runKsp(featureAwareProcessor, changed = listOf("other.Unrelated"), libraries = listOf(emptyLibDir))
        assertEquals("gen A", File(out, "kotlin/p1/AGen.kt").readText())
        assertFalse(File(out, "kotlin/p1/FeatureConfig.kt").exists())

        // Run 6: subsequent unrelated classpath change exits early and restores only AGen.kt.
        out.deleteRecursively()
        runKsp(failing, changed = listOf("other.Unrelated"), libraries = listOf(emptyLibDir))
        assertEquals("gen A", File(out, "kotlin/p1/AGen.kt").readText())
        assertFalse(File(out, "kotlin/p1/FeatureConfig.kt").exists())
    }

    @Test
    fun `associating synthetic members of source classes stays isolating`() {
        val j = src("p1/J.java").apply {
            writeText("package p1; public class J {}")
        }
        val e = src("p1/E.kt").apply {
            writeText("package p1\ninterface Base { val prop: Int }\nclass E(b: Base) : Base by b")
        }
        val syntheticAssocProcessor = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                when (file.fileName) {
                    "J.java" -> {
                        val jClass = file.declarations
                            .filterIsInstance<com.google.devtools.ksp.symbol.KSClassDeclaration>()
                            .single()
                        val ctor = jClass.getConstructors().single()
                        assertEquals(Origin.SYNTHETIC, ctor.origin)
                        codeGenerator.createNewFile(Dependencies(false), "p1", "JGen").use {
                            it.write("gen J".toByteArray())
                        }
                        codeGenerator.associateWithFunctions(listOf(ctor), "p1", "JGen")
                    }
                    "E.kt" -> {
                        val eClass = file.declarations
                            .filterIsInstance<com.google.devtools.ksp.symbol.KSClassDeclaration>()
                            .single { it.simpleName.asString() == "E" }
                        val delegatedProp = eClass.getAllProperties()
                            .single { it.simpleName.asString() == "prop" }
                        assertEquals(Origin.SYNTHETIC, delegatedProp.origin)
                        codeGenerator.createNewFile(Dependencies(false), "p1", "EGen").use {
                            it.write("gen E".toByteArray())
                        }
                        codeGenerator.associateWithProperties(listOf(delegatedProp), "p1", "EGen")
                    }
                }
            }
        }
        runKsp(syntheticAssocProcessor)
        out.deleteRecursively()

        val failing = SymbolProcessorProvider { error("synthetic source member association must stay isolating") }
        runKsp(failing, changed = listOf("other.Unrelated"))
        assertEquals("gen J", File(out, "kotlin/p1/JGen.kt").readText())
        assertEquals("gen E", File(out, "kotlin/p1/EGen.kt").readText())
        assertFalse(canExitEarly(modified = listOf(j)))
        assertFalse(canExitEarly(modified = listOf(e)))
    }

    @Test
    fun `KSP normalizes dollar signs in class and package queries to match classpath change names`() {
        val libDir = compileJavaLibrary("hints/Bar\$Generated.java" to "package hints; public class Bar\$Generated {}")
        val dollarQueryProcessor = processor { resolver, codeGenerator ->
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.Foo\$Hint"))
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.User.\$serializer"))
            resolver.getPropertyDeclarationByName(
                resolver.getKSNameFromString("lib.Missing\$Serializer.descriptor"),
                includeTopLevel = true,
            )
            resolver.getFunctionDeclarationsByName(
                resolver.getKSNameFromString("lib.Missing\$Helper.run"),
                includeTopLevel = true,
            ).toList()
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            resolver.getDeclarationsFromPackage("hints").toList()
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            resolver.getDeclarationsFromPackage("pkg\$1").toList()
            val files = resolver.getAllFiles().toList().toTypedArray()
            codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
        }
        runKsp(dollarQueryProcessor, libraries = listOf(libDir))

        // Both '$' (from KspSubplugin.toChangedClassName) and dot-normalized changedClasses inputs must match.
        assertFalse(canExitEarly(changed = listOf("lib.Foo\$Hint")))
        assertFalse(canExitEarly(changed = listOf("lib.Foo.Hint")))
        assertFalse(canExitEarly(changed = listOf("lib.User\$\$serializer")))
        assertFalse(canExitEarly(changed = listOf("lib.User..serializer")))
        assertFalse(canExitEarly(changed = listOf("hints.Bar\$Generated")))
        assertFalse(canExitEarly(changed = listOf("hints.Bar.Generated")))
        assertFalse(canExitEarly(changed = listOf("lib.Missing\$Serializer")))
        assertFalse(canExitEarly(changed = listOf("lib.Missing.Serializer")))
        assertFalse(canExitEarly(changed = listOf("lib.Missing\$Helper")))
        assertFalse(canExitEarly(changed = listOf("lib.Missing.Helper")))
        assertFalse(canExitEarly(changed = listOf("pkg\$1.Foo")))
        assertFalse(canExitEarly(changed = listOf("pkg.1.Foo")))
        assertTrue(canExitEarly(changed = listOf("hints.sub.Unrelated")))
        assertTrue(canExitEarly(changed = listOf("pkg\$1.sub.Unrelated")))
        assertTrue(canExitEarly(changed = listOf("pkg.1.sub.Unrelated")))
    }

    @Test
    fun `PackageQueries matches newly added dollar-named classes and file facades without matching subpackages`() {
        val pkgProcessor = processor { resolver, codeGenerator ->
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            resolver.getDeclarationsFromPackage("empty_hints").toList()
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            resolver.getDeclarationsFromPackage("").toList()
            resolver.getFunctionDeclarationsByName(
                resolver.getKSNameFromString("fn_pkg.topFn"),
                includeTopLevel = true,
            ).toList()
            val files = resolver.getAllFiles().toList().toTypedArray()
            codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
        }
        runKsp(pkgProcessor)

        // KspSubplugin converts '/' to '.' while preserving '$' so package vs. class boundaries stay distinct.
        assertFalse(canExitEarly(changed = listOf("empty_hints.Bar\$Generated")), "newly added '$'-named class")
        assertFalse(canExitEarly(changed = listOf("empty_hints.\$AggregatedHint")), "leading-'$' class in package")
        assertFalse(canExitEarly(changed = listOf("fn_pkg.My\$UtilKt")), "'$'-named Kotlin file facade")
        assertFalse(canExitEarly(changed = listOf("Root\$Generated")), "newly added '$'-named class in root package")

        // Subpackages must still be ignored.
        assertTrue(canExitEarly(changed = listOf("empty_hints.Bar.Generated")))
        assertTrue(canExitEarly(changed = listOf("empty_hints.sub.Other\$Inner")))
        assertTrue(canExitEarly(changed = listOf("fn_pkg.sub.My\$UtilKt")))
    }

    @Test
    fun `enclosing or nested binary class changes prune NoSourceFile outputs instead of restoring backups`() {
        val enumAndNestedProcessor = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                codeGenerator.createNewFile(Dependencies(false, file), "p1", "AGen").use {
                    it.write("gen A".toByteArray())
                }
            }
            val entry = resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.MyEnum.ENTRY"))
            if (entry != null) {
                codeGenerator.createNewFile(Dependencies(false), "p1", "EntryConfig").use {
                    it.write("entry=ENTRY".toByteArray())
                }
                codeGenerator.associateWithClasses(listOf(entry), "p1", "EntryConfig")
            }
            @OptIn(com.google.devtools.ksp.KspExperimental::class)
            val outer = resolver.getDeclarationsFromPackage("hints")
                .filterIsInstance<com.google.devtools.ksp.symbol.KSClassDeclaration>()
                .singleOrNull { it.simpleName.asString() == "Outer" }
            if (outer != null && outer.declarations.any { it.simpleName.asString() == "Inner" }) {
                codeGenerator.createNewFile(Dependencies(false), "p1", "NestedConfig").use {
                    it.write("nested=Inner".toByteArray())
                }
                codeGenerator.associateWithClasses(listOf(outer), "p1", "NestedConfig")
            }
        }

        // Run 1: both lib.MyEnum.ENTRY and hints.Outer.Inner exist on the classpath.
        val initialLib = compileJavaLibrary(
            "lib/MyEnum.java" to "package lib; public enum MyEnum { ENTRY }",
            "hints/Outer.java" to "package hints; public class Outer { public static class Inner {} }",
        )
        runKsp(enumAndNestedProcessor, libraries = listOf(initialLib))
        assertEquals("entry=ENTRY", File(out, "kotlin/p1/EntryConfig.kt").readText())
        assertEquals("nested=Inner", File(out, "kotlin/p1/NestedConfig.kt").readText())

        // Run 2: ENTRY is removed from MyEnum (changedClasses = ["lib.MyEnum"]) and Inner is removed from Outer
        // (changedClasses = ["hints.Outer.Inner"]). Neither stale backup may be restored.
        val updatedLib = compileJavaLibrary(
            "lib/MyEnum.java" to "package lib; public enum MyEnum {}",
            "hints/Outer.java" to "package hints; public class Outer {}",
        )
        out.deleteRecursively()
        runKsp(
            enumAndNestedProcessor,
            changed = listOf("lib.MyEnum", "hints.Outer.Inner"),
            libraries = listOf(updatedLib),
        )
        assertEquals("gen A", File(out, "kotlin/p1/AGen.kt").readText())
        assertFalse(
            File(out, "kotlin/p1/EntryConfig.kt").exists(),
            "EntryConfig.kt must not be restored after enclosing enum lib.MyEnum removes ENTRY",
        )
        assertFalse(
            File(out, "kotlin/p1/NestedConfig.kt").exists(),
            "NestedConfig.kt must not be restored after nested class hints.Outer.Inner is removed",
        )

        // Run 3 & 4: drain removedOutputsKey, then verify early exit keeps both stale outputs absent.
        out.deleteRecursively()
        runKsp(enumAndNestedProcessor, changed = listOf("other.Unrelated"), libraries = listOf(updatedLib))
        out.deleteRecursively()
        val failing = SymbolProcessorProvider { error("processors must not run on clean build") }
        runKsp(failing, changed = listOf("other.Unrelated"), libraries = listOf(updatedLib))
        assertEquals("gen A", File(out, "kotlin/p1/AGen.kt").readText())
        assertFalse(File(out, "kotlin/p1/EntryConfig.kt").exists())
        assertFalse(File(out, "kotlin/p1/NestedConfig.kt").exists())
    }

    @Test
    fun `internal Java annotation value resolution does not pollute classQueries`() {
        val libDir = compileJavaLibrary(
            "lib/Anno.java" to "package lib; public @interface Anno { Class<?> prim(); Class<?> cls(); }",
            "lib/LibTarget.java" to "package lib; public class LibTarget {}",
        )
        val j = src("p1/J.java").apply {
            writeText("package p1; @lib.Anno(prim = int.class, cls = lib.LibTarget.class) public class J {}")
        }
        val inspectAnnoProcessor = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                for (decl in file.declarations) {
                    decl.annotations.forEach { anno -> anno.arguments.forEach { it.value } }
                }
                val outName = file.fileName.substringBefore('.') + "Gen"
                codeGenerator.createNewFile(Dependencies(false, file), "p1", outName).use {
                    it.write("gen".toByteArray())
                }
            }
        }
        runKsp(inspectAnnoProcessor, libraries = listOf(libDir))

        // Boxed primitive lookup is internal KSP plumbing; LibTarget is tracked per-file via symbolLookupCache.
        assertEquals(emptySet<String>(), ClassQueries(File(caches, "classQueries")).toSet())
        assertTrue(canExitEarly(changed = listOf("java.lang.Integer")))
        assertFalse(canExitEarly(changed = listOf("lib.LibTarget")))

        // Once J.java is removed (and JGen.kt disappearance is drained), early exit succeeds for lib.LibTarget.
        assertTrue(j.delete())
        out.deleteRecursively()
        runKsp(inspectAnnoProcessor, removed = listOf(j), libraries = listOf(libDir))
        out.deleteRecursively()
        runKsp(inspectAnnoProcessor, libraries = listOf(libDir))

        assertTrue(
            canExitEarly(changed = listOf("lib.LibTarget")),
            "lib.LibTarget must not remain stuck in module-wide classQueries after J.java is removed",
        )
    }

    @Test
    fun `associateWithClasses preserves dollar signs in NoSourceFile for DirtinessPropagator`() {
        val libDir = compileJavaLibrary("lib/Bar\$Generated.java" to "package lib; public class Bar\$Generated {}")
        val b = src("p1/B.java").apply {
            writeText("package p1; public class B extends lib.Bar\$Generated {}")
        }
        val seenFiles = mutableListOf<String>()
        val proc = processor { resolver, codeGenerator ->
            seenFiles.clear()
            for (file in resolver.getAllFiles()) {
                seenFiles.add(file.fileName)
                if (file.fileName == "B.java") {
                    for (decl in file.declarations) {
                        if (decl is com.google.devtools.ksp.symbol.KSClassDeclaration) {
                            decl.superTypes.forEach { it.resolve() }
                        }
                    }
                }
            }
            if ("A.kt" in seenFiles) {
                val aFile = resolver.getAllFiles().single { it.fileName == "A.kt" }
                val barCls = resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.Bar\$Generated"))!!
                codeGenerator.createNewFile(Dependencies(false, aFile), "p1", "Out").use {
                    it.write("out".toByteArray())
                }
                codeGenerator.associateWithClasses(listOf(barCls), "p1", "Out")
            }
        }
        runKsp(proc, libraries = listOf(libDir))
        assertTrue("B.java" in seenFiles)

        // Modifying A.kt dirties Out.kt -> NoSourceFile(lib.Bar$Generated) -> B.java via symbolLookupCache.
        a.writeText("package p1\nclass A2")
        out.deleteRecursively()
        runKsp(proc, modified = listOf(a), libraries = listOf(libDir))
        assertTrue(
            "B.java" in seenFiles,
            "DirtinessPropagator must propagate from NoSourceFile(lib.Bar\$Generated) to B.java, saw: $seenFiles",
        )
        assertTrue(b.exists())
    }

    @Test
    fun `DirtinessPropagator propagates from JAVA_LIB class-literal annotation lookup through NoSourceFile`() {
        val libDir = compileJavaLibrary(
            "lib/RefTarget.java" to "package lib; public class RefTarget {}",
            "lib/HolderAnno.java" to "package lib; public @interface HolderAnno { Class<?> value(); }",
            "lib/LibHolder.java" to "package lib; @HolderAnno(RefTarget.class) public class LibHolder {}",
        )
        val seenFiles = mutableListOf<String>()
        val proc = processor { resolver, codeGenerator ->
            seenFiles.clear()
            for (file in resolver.getAllFiles()) {
                seenFiles.add(file.fileName)
                if (file.fileName == "A.kt") {
                    val holder = resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.LibHolder"))!!
                    holder.annotations.forEach { anno -> anno.arguments.forEach { it.value } }
                    codeGenerator.createNewFile(Dependencies(false, file), "p1", "Out").use {
                        it.write("out".toByteArray())
                    }
                    codeGenerator.associateWithClasses(listOf(holder), "p1", "Out")
                }
            }
        }
        runKsp(proc, libraries = listOf(libDir))
        assertTrue("A.kt" in seenFiles)

        // When lib.RefTarget changes (lib.LibHolder unchanged), DirtinessPropagator must follow
        // ("lib", "RefTarget") -> NoSourceFile(lib.LibHolder) -> Out.kt -> A.kt.
        out.deleteRecursively()
        runKsp(proc, changed = listOf("lib.RefTarget"), libraries = listOf(libDir))
        assertTrue(
            "A.kt" in seenFiles,
            "DirtinessPropagator must propagate from JAVA_LIB NoSourceFile to A.kt, saw: $seenFiles",
        )
    }

    @Test
    fun `DirtinessPropagator propagates from JAVA_LIB constructor and method annotations through enclosing NoSourceFile`() {
        val libDir = compileJavaLibrary(
            "lib/CtorTarget.java" to "package lib; public class CtorTarget {}",
            "lib/MethodTarget.java" to "package lib; public class MethodTarget {}",
            "lib/HolderAnno.java" to "package lib; public @interface HolderAnno { Class<?> value(); }",
            "lib/LibHolder.java" to """
                package lib;
                public class LibHolder {
                    @HolderAnno(CtorTarget.class) public LibHolder() {}
                    @HolderAnno(MethodTarget.class) public void foo() {}
                }
            """.trimIndent(),
        )
        val seenFiles = mutableListOf<String>()
        val proc = processor { resolver, codeGenerator ->
            seenFiles.clear()
            for (file in resolver.getAllFiles()) {
                seenFiles.add(file.fileName)
                if (file.fileName == "A.kt") {
                    val holder = resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.LibHolder"))!!
                    holder.declarations.forEach { member ->
                        member.annotations.forEach { anno -> anno.arguments.forEach { it.value } }
                    }
                    codeGenerator.createNewFile(Dependencies(false, file), "p1", "Out").use {
                        it.write("out".toByteArray())
                    }
                    codeGenerator.associateWithClasses(listOf(holder), "p1", "Out")
                }
            }
        }
        runKsp(proc, libraries = listOf(libDir))
        assertTrue("A.kt" in seenFiles)

        out.deleteRecursively()
        runKsp(proc, changed = listOf("lib.CtorTarget"), libraries = listOf(libDir))
        assertTrue(
            "A.kt" in seenFiles,
            "DirtinessPropagator must propagate from JAVA_LIB constructor annotation to A.kt, saw: $seenFiles",
        )

        out.deleteRecursively()
        runKsp(proc, changed = listOf("lib.MethodTarget"), libraries = listOf(libDir))
        assertTrue(
            "A.kt" in seenFiles,
            "DirtinessPropagator must propagate from JAVA_LIB method annotation to A.kt, saw: $seenFiles",
        )
    }

    @Test
    fun `DirtinessPropagator propagates from binary enum entry and nested class annotations through enclosing NoSourceFile`() {
        val libDir = compileJavaLibrary(
            "lib/EntryTarget.java" to "package lib; public class EntryTarget {}",
            "lib/NestedTarget.java" to "package lib; public class NestedTarget {}",
            "lib/HolderAnno.java" to "package lib; public @interface HolderAnno { Class<?> value(); }",
            "lib/LibEnum.java" to """
                package lib;
                public enum LibEnum {
                    @HolderAnno(EntryTarget.class) ENTRY
                }
            """.trimIndent(),
            "lib/LibHolder.java" to """
                package lib;
                public class LibHolder {
                    @HolderAnno(NestedTarget.class) public static class Inner {}
                }
            """.trimIndent(),
        )
        val seenFiles = mutableListOf<String>()
        val proc = processor { resolver, codeGenerator ->
            seenFiles.clear()
            for (file in resolver.getAllFiles()) {
                seenFiles.add(file.fileName)
                if (file.fileName == "A.kt") {
                    val libEnum = resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.LibEnum"))!!
                    val libHolder = resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.LibHolder"))!!
                    (libEnum.declarations + libHolder.declarations).forEach { member ->
                        member.annotations.forEach { anno -> anno.arguments.forEach { it.value } }
                    }
                    codeGenerator.createNewFile(Dependencies(false, file), "p1", "Out").use {
                        it.write("out".toByteArray())
                    }
                    codeGenerator.associateWithClasses(listOf(libEnum, libHolder), "p1", "Out")
                }
            }
        }
        runKsp(proc, libraries = listOf(libDir))
        assertTrue("A.kt" in seenFiles)

        out.deleteRecursively()
        runKsp(proc, changed = listOf("lib.EntryTarget"), libraries = listOf(libDir))
        assertTrue(
            "A.kt" in seenFiles,
            "DirtinessPropagator must propagate from binary enum entry annotation to A.kt, saw: $seenFiles",
        )
        assertFalse(
            canExitEarly(changed = listOf("lib.EntryTarget")),
            "Lookup for binary enum entry must survive Run 2 without cache erasure",
        )

        out.deleteRecursively()
        runKsp(proc, changed = listOf("lib.NestedTarget"), libraries = listOf(libDir))
        assertTrue(
            "A.kt" in seenFiles,
            "DirtinessPropagator must propagate from binary nested class annotation to A.kt, saw: $seenFiles",
        )
    }

    @Test
    fun `modifying Kotlin, Java, and binary inputs to drop class references prunes stale lookups`() {
        val libDirV1 = compileJavaLibrary(
            "lib/OldTarget.java" to "package lib; public class OldTarget {}",
            "lib/OldTarget2.java" to "package lib; public class OldTarget2 {}",
            "lib/OldTarget3.java" to "package lib; public class OldTarget3 {}",
            "lib/Anno.java" to "package lib; public @interface Anno { Class<?> value(); }",
            "lib/LibHolder.java" to "package lib; @Anno(OldTarget3.class) public class LibHolder {}",
        )
        a.writeText("package p1\n@lib.Anno(lib.OldTarget::class)\nclass A")
        val j = src("p1/J.java").apply {
            writeText("package p1;\n@lib.Anno(lib.OldTarget2.class)\npublic class J {}")
        }
        val proc = processor { resolver, codeGenerator ->
            for (file in resolver.getAllFiles()) {
                for (decl in file.declarations) {
                    decl.annotations.forEach { anno -> anno.arguments.forEach { it.value } }
                }
                val outName = file.fileName.substringBefore('.') + "Gen"
                codeGenerator.createNewFile(Dependencies(false, file), "p1", outName).use {
                    it.write("gen".toByteArray())
                }
                if (file.fileName == "A.kt") {
                    val holder = resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.LibHolder"))!!
                    holder.annotations.forEach { anno -> anno.arguments.forEach { it.value } }
                    codeGenerator.associateWithClasses(listOf(holder), "p1", outName)
                }
            }
        }
        runKsp(proc, libraries = listOf(libDirV1))
        assertFalse(canExitEarly(changed = listOf("lib.OldTarget")))
        assertFalse(canExitEarly(changed = listOf("lib.OldTarget2")))
        assertFalse(canExitEarly(changed = listOf("lib.OldTarget3")))

        // Run 2: modify A.kt, J.java, and LibHolder to drop references to OldTarget / OldTarget2 / OldTarget3.
        val libDirV2 = compileJavaLibrary(
            "lib/OldTarget.java" to "package lib; public class OldTarget {}",
            "lib/OldTarget2.java" to "package lib; public class OldTarget2 {}",
            "lib/OldTarget3.java" to "package lib; public class OldTarget3 {}",
            "lib/Anno.java" to "package lib; public @interface Anno { Class<?> value(); }",
            "lib/LibHolder.java" to "package lib; public class LibHolder {}",
        )
        a.writeText("package p1\nclass A")
        j.writeText("package p1;\npublic class J {}")
        runKsp(proc, modified = listOf(a, j), changed = listOf("lib.LibHolder"), libraries = listOf(libDirV2))

        assertTrue(canExitEarly(changed = listOf("lib.OldTarget")), "stale Kotlin lookup must be pruned on edit")
        assertTrue(canExitEarly(changed = listOf("lib.OldTarget2")), "stale Java lookup must be pruned on edit")
        assertTrue(canExitEarly(changed = listOf("lib.OldTarget3")), "stale NoSourceFile lookup must be pruned on change")
    }

    @Test
    fun `binary supertypes propagate through NoSourceFile`() {
        val libDir = compileJavaLibrary(
            "other/SuperType.java" to "package other; public class SuperType {}",
            "lib/Holder.java" to "package lib; public class Holder extends other.SuperType {}",
        )
        val seenFiles = mutableListOf<String>()
        val proc = processor { resolver, codeGenerator ->
            seenFiles.clear()
            for (file in resolver.getAllFiles()) {
                seenFiles.add(file.fileName)
                if (file.fileName == "A.kt") {
                    val holder = resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.Holder"))!!
                    // Resolve binary superTypes (triggers recordLookup with JAVA_LIB ClsFileImpl).
                    holder.superTypes.forEach { it.resolve() }
                    codeGenerator.createNewFile(Dependencies(false, file), "p1", "Out").use {
                        it.write("out".toByteArray())
                    }
                    codeGenerator.associateWithClasses(listOf(holder), "p1", "Out")
                }
            }
        }
        runKsp(proc, libraries = listOf(libDir))
        assertTrue("A.kt" in seenFiles)

        out.deleteRecursively()
        runKsp(proc, changed = listOf("other.SuperType"), libraries = listOf(libDir))
        assertTrue(
            "A.kt" in seenFiles,
            "DirtinessPropagator must propagate from binary superType other.SuperType to A.kt, saw: $seenFiles",
        )
    }

    @Test
    fun `Resolver API queries for inherited nested class record resolved declaring class`() {
        val libDir = compileJavaLibrary(
            "lib/Base.java" to "package lib; public class Base { public static class Inner { public void foo() {} } }",
            "lib/Sub.java" to "package lib; public class Sub extends Base {}",
        )
        val proc = processor { resolver, codeGenerator ->
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("lib.Sub.Inner"))
            for (file in resolver.getAllFiles()) {
                codeGenerator.createNewFile(Dependencies(false, file), "p1", "AGen").use {
                    it.write("gen".toByteArray())
                }
            }
        }
        runKsp(proc, libraries = listOf(libDir))

        assertFalse(
            canExitEarly(changed = listOf("lib.Base\$Inner")),
            "Querying lib.Sub.Inner must also record resolved declaring class lib.Base.Inner",
        )
    }

    @Test
    fun `PackageQueries matches enclosing subclass added for includeTopLevel callable query without matching subpackages`() {
        val libDir = compileJavaLibrary(
            "lib/Base.java" to "package lib; public class Base { public static class Inner { public void foo() {} } }",
        )
        val proc = processor { resolver, codeGenerator ->
            resolver.getFunctionDeclarationsByName(
                resolver.getKSNameFromString("lib.MissingSub.Inner.foo"),
                includeTopLevel = true,
            ).toList()
            for (file in resolver.getAllFiles()) {
                codeGenerator.createNewFile(Dependencies(false, file), "p1", "AGen").use {
                    it.write("gen".toByteArray())
                }
            }
        }
        runKsp(proc, libraries = listOf(libDir))

        assertFalse(
            canExitEarly(changed = listOf("lib.MissingSub")),
            "Adding lib.MissingSub (which may inherit Inner.foo) must invalidate includeTopLevel query",
        )
        assertTrue(
            canExitEarly(changed = listOf("lib.MissingSub.Inner.sub.Other")),
            "Subpackages of the queried qualifier must not invalidate PackageQueries",
        )
        assertTrue(
            canExitEarly(changed = listOf("lib.Unrelated")),
            "Sibling classes in parent package must not invalidate PackageQueries",
        )
    }

    @Test
    fun `Resolver API queries on existing subclass record supertype and intermediate nested class queries`() {
        val libDir = compileJavaLibrary(
            "lib/Base.java" to "package lib; public class Base {}",
            "lib/BaseWithInner.java" to "package lib; public class BaseWithInner { public static class Inner {} }",
            "lib/Mid.java" to "package lib; public class Mid extends BaseWithInner {}",
            "lib/BaseOuter.java" to "package lib; public class BaseOuter { public static class Mid {} }",
        )
        src("p1/Sub.java").writeText("package p1; public class Sub extends lib.Base {}")
        src("p1/Sub2.java").writeText("package p1; public class Sub2 extends lib.Mid {}")
        src("p1/Outer.java").writeText("package p1; public class Outer extends lib.BaseOuter {}")

        val proc = processor { resolver, codeGenerator ->
            resolver.getFunctionDeclarationsByName(
                resolver.getKSNameFromString("p1.Sub.Inner.foo"),
                includeTopLevel = true,
            ).toList()
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("p1.Sub2.Inner"))
            resolver.getClassDeclarationByName(resolver.getKSNameFromString("p1.Outer.Mid.Leaf"))
            for (file in resolver.getAllFiles()) {
                val outName = file.fileName.substringBefore('.') + "Gen"
                codeGenerator.createNewFile(Dependencies(false, file), "p1", outName).use {
                    it.write("gen".toByteArray())
                }
            }
        }
        runKsp(proc, libraries = listOf(libDir))

        assertFalse(
            canExitEarly(changed = listOf("lib.Base", "lib.Base\$Inner")),
            "Adding Inner to lib.Base must invalidate p1.Sub.Inner.foo query",
        )
        assertFalse(
            canExitEarly(changed = listOf("lib.Mid", "lib.Mid\$Inner")),
            "Shadowing Inner in intermediate supertype lib.Mid must invalidate p1.Sub2.Inner query",
        )
        assertFalse(
            canExitEarly(changed = listOf("lib.BaseOuter\$Mid", "lib.BaseOuter\$Mid\$Leaf")),
            "Adding Leaf to inherited lib.BaseOuter\$Mid must invalidate p1.Outer.Mid.Leaf query",
        )
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

    /** Generates AGen.kt from all files. */
    private val generatesAGen = processor { resolver, codeGenerator ->
        val files = resolver.getAllFiles().toList().toTypedArray()
        codeGenerator.createNewFile(Dependencies(false, *files), "p1", "AGen").use { it.write("gen".toByteArray()) }
    }

    private fun compileJavaLibrary(vararg sources: Pair<String, String>): File {
        val srcDir = File(tmp, "libSrc").apply {
            deleteRecursively()
            mkdirs()
        }
        val outDir = File(tmp, "libClasses").apply {
            deleteRecursively()
            mkdirs()
        }
        if (sources.isEmpty()) return outDir
        val javaFiles = sources.map { (relPath, content) ->
            File(srcDir, relPath).apply {
                parentFile.mkdirs()
                writeText(content)
            }
        }
        val compiler = ToolProvider.getSystemJavaCompiler()
        val args = listOf("-d", outDir.path) + javaFiles.map { it.path }
        assertEquals(0, compiler.run(null, null, null, *args.toTypedArray()), "javac failed")
        return outDir
    }

    private fun runKsp(
        provider: SymbolProcessorProvider,
        modified: List<File> = emptyList(),
        removed: List<File> = emptyList(),
        changed: List<String> = emptyList(),
        libraries: List<File> = emptyList(),
        log: Boolean = false,
        experimentalPsiResolution: Boolean = false,
    ) = runKsp(Supplier { listOf(provider) }, modified, removed, changed, libraries, log, experimentalPsiResolution)

    private fun runKsp(
        providers: Supplier<List<SymbolProcessorProvider>>,
        modified: List<File> = emptyList(),
        removed: List<File> = emptyList(),
        changed: List<String> = emptyList(),
        libraries: List<File> = emptyList(),
        log: Boolean = false,
        experimentalPsiResolution: Boolean = false,
    ) {
        val exitCode = KotlinSymbolProcessing(
            config(modified, removed, changed, libraries, log, experimentalPsiResolution),
            providers,
            CommandLineKSPLogger(),
        ).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exitCode)
    }

    private fun config(
        modified: List<File> = emptyList(),
        removed: List<File> = emptyList(),
        changed: List<String> = emptyList(),
        libraries: List<File> = emptyList(),
        log: Boolean = false,
        experimentalPsiResolution: Boolean = false,
    ): KSPJvmConfig {
        val srcRoot = a.parentFile.parentFile
        val libRoots = libraries
        return KSPJvmConfig.Builder().apply {
            moduleName = "test"
            sourceRoots = listOf(srcRoot)
            javaSourceRoots = listOf(srcRoot) // Overlaps with sourceRoots, as in a typical Gradle setup.
            commonSourceRoots = emptyList()
            this.libraries = libRoots
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
            this.experimentalPsiResolution = experimentalPsiResolution
            modifiedSources = modified
            removedSources = removed
            changedClasses = changed
        }.build()
    }

    /** Serializes [config] like the Gradle plugin does for [KSPLoader]. */
    private fun serialized(config: KSPJvmConfig): ByteArray =
        ByteArrayOutputStream().also { bytes -> ObjectOutputStream(bytes).use { it.writeObject(config) } }.toByteArray()

    /** Files in [dir] that this process has open. Skips the test where the OS doesn't tell (it needs /proc). */
    private fun openFilesIn(dir: File): List<String> {
        val fileDescriptors = File("/proc/self/fd")
        assumeTrue(fileDescriptors.isDirectory, "needs /proc/self/fd to list open files")
        val dirPath = dir.canonicalPath + File.separator
        return fileDescriptors.listFiles().orEmpty()
            .mapNotNull { runCatching { it.canonicalPath }.getOrNull() }
            .filter { it.startsWith(dirPath) }
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
