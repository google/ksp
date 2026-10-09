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
import com.google.devtools.ksp.common.NoSourceFile
import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.DualLookupTracker
import com.google.devtools.ksp.impl.IncrementalContextAA
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@Suppress("DEPRECATION")
class IncrementalEarlyExitTest {
    private class FakeKSName(private val str: String) : KSName {
        override fun asString(): String = str
        override fun getQualifier(): String = str.substringBeforeLast('.', "")
        override fun getShortName(): String = str.substringAfterLast('.')
    }

    private class FakeKSClassDeclaration(
        private val simple: String,
        private val qualified: String,
        override val containingFile: KSFile?,
    ) : KSClassDeclaration {
        override val simpleName: KSName = FakeKSName(simple)
        override val qualifiedName: KSName = FakeKSName(qualified)
        override val typeParameters: List<KSTypeParameter> = emptyList()
        override val packageName: KSName = FakeKSName(qualified.substringBeforeLast('.', ""))
        override val parentDeclaration: KSDeclaration? = null
        override val docString: String? = null
        override val modifiers: Set<Modifier> = emptySet()
        override val origin: Origin = Origin.KOTLIN
        override val location: Location = FileLocation("", 0)
        override val parent: KSNode? = containingFile
        override val annotations: Sequence<KSAnnotation> = emptySequence()
        override val isActual: Boolean = false
        override val isExpect: Boolean = false
        override fun findActuals(): Sequence<KSDeclaration> = emptySequence()
        override fun findExpects(): Sequence<KSDeclaration> = emptySequence()
        override val classKind: ClassKind = ClassKind.CLASS
        override val primaryConstructor: KSFunctionDeclaration? = null
        override val superTypes: Sequence<KSTypeReference> = emptySequence()
        override val isCompanionObject: Boolean = false
        override fun getSealedSubclasses(): Sequence<KSClassDeclaration> = emptySequence()
        override fun getAllFunctions(): Sequence<KSFunctionDeclaration> = emptySequence()
        override fun getAllProperties(): Sequence<KSPropertyDeclaration> = emptySequence()
        override fun asType(typeArguments: List<KSTypeArgument>): KSType = throw UnsupportedOperationException()
        override fun asStarProjectedType(): KSType = throw UnsupportedOperationException()
        override val declarations: Sequence<KSDeclaration> = emptySequence()
        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = visitor.visitClassDeclaration(this, data)
    }

    private class FakeKSFile(
        override val filePath: String,
        pkg: String,
        declNames: List<String>,
    ) : KSFile {
        override val fileName: String = File(filePath).name
        override val packageName: KSName = FakeKSName(pkg)
        override val declarations: Sequence<KSDeclaration> =
            declNames.asSequence().map { FakeKSClassDeclaration(it, "$pkg.$it", this) }
        override val annotations: Sequence<KSAnnotation> = emptySequence()
        override val origin: Origin = Origin.KOTLIN
        override val location: Location = FileLocation(filePath, 1)
        override val parent: KSNode? = null
        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = visitor.visitFile(this, data)
    }

    @Test
    fun testTryEarlyExitIfCleanWithNoSourceFileAndLookups(@TempDir tempDir: File) {
        val baseDir = File(tempDir, "project").apply { mkdirs() }
        val cachesDir = File(tempDir, "caches").apply { mkdirs() }
        val outputDir = File(tempDir, "out").apply { mkdirs() }
        val anyChangesWildcard = File(baseDir, "<any>").relativeTo(baseDir)
        val loggingOpts = IncrementalContextLoggingOptions(false, null)

        val fileA = File(baseDir, "src/main/kotlin/p1/A.kt").apply {
            parentFile.mkdirs()
            writeText("package p1\nclass A")
        }
        val outA = File(outputDir, "kotlin/p1/AGen.kt").apply {
            parentFile.mkdirs()
            writeText("package p1\nclass AGen")
        }
        val noSourceL4 = NoSourceFile(baseDir, "p1.L4")
        val ksA = FakeKSFile(fileA.absolutePath, "p1", listOf("A"))

        // Build 1: clean build, record lookups of p1.UsedClass and p1.UsedSymbol, and associate output with NoSourceFile(p1.L4)
        run {
            val tracker = DualLookupTracker()
            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = tracker,
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            assertFalse(ctx.tryEarlyExitIfClean(), "Should not early-exit before initial build caches exist")
            val dirty = ctx.calcDirtyFiles(listOf(ksA))
            tracker.classTracker.record(fileA.absolutePath, Position.NO_POSITION, "p1", ScopeKind.PACKAGE, "UsedClass")
            tracker.symbolTracker.record(fileA.absolutePath, Position.NO_POSITION, "p1", ScopeKind.PACKAGE, "UsedSymbol")
            val s2o = mapOf(
                fileA.relativeTo(baseDir) to setOf(outA.relativeTo(baseDir)),
                File(noSourceL4.filePath).relativeTo(baseDir) to setOf(outA.relativeTo(baseDir)),
            )
            ctx.updateCachesAndOutputs(dirty, setOf(outA), s2o)
            ctx.closeFiles()
        }

        // Case 1: unrelated class changes on classpath -> tryEarlyExitIfClean returns true even with NoSourceFile(p1.L4) present
        run {
            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = listOf("p1.UnrelatedClass"),
            )
            assertTrue(ctx.tryEarlyExitIfClean(), "Should early-exit when changedClasses misses both lookups and NoSourceFile")
            ctx.closeFiles()
        }

        // Case 2: class matching NoSourceFile(p1.L4) changes -> tryEarlyExitIfClean returns false
        run {
            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = listOf("p1.L4"),
            )
            assertFalse(ctx.tryEarlyExitIfClean(), "Should NOT early-exit when changedClasses hits NoSourceFile(p1.L4)")
            ctx.closeFiles()
        }

        // Case 3: class matching recorded class lookup p1.UsedClass changes -> tryEarlyExitIfClean returns false
        run {
            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = listOf("p1.UsedClass"),
            )
            assertFalse(ctx.tryEarlyExitIfClean(), "Should NOT early-exit when changedClasses hits recorded class lookup")
            ctx.closeFiles()
        }

        // Case 4: class matching recorded symbol lookup p1.UsedSymbol changes -> tryEarlyExitIfClean returns false
        run {
            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = listOf("p1.UsedSymbol"),
            )
            assertFalse(ctx.tryEarlyExitIfClean(), "Should NOT early-exit when changedClasses hits recorded symbol lookup")
            ctx.closeFiles()
        }
    }

    @Test
    fun testTryEarlyExitIfCleanNegativePredicatesAndOutputRestoration(@TempDir tempDir: File) {
        val baseDir = File(tempDir, "project").apply { mkdirs() }
        val cachesDir = File(tempDir, "caches").apply { mkdirs() }
        val outputDir = File(tempDir, "out").apply { mkdirs() }
        val anyChangesWildcard = File(baseDir, "<any>").relativeTo(baseDir)
        val loggingOpts = IncrementalContextLoggingOptions(true, null)

        val srcRoot = File(baseDir, "src/main/kotlin").apply { mkdirs() }
        val fileA = File(srcRoot, "p1/A.kt").apply {
            parentFile.mkdirs()
            writeText("package p1\nclass A")
        }
        val fileB = File(srcRoot, "p1/B.kt").apply {
            parentFile.mkdirs()
            writeText("package p1\nclass B")
        }
        val outA = File(outputDir, "kotlin/p1/AGen.kt").apply {
            parentFile.mkdirs()
            writeText("package p1\nclass AGen")
        }
        val ksA = FakeKSFile(fileA.absolutePath, "p1", listOf("A"))
        val ksB = FakeKSFile(fileB.absolutePath, "p1", listOf("B"))

        // Initial clean build
        run {
            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            val dirty = ctx.calcDirtyFiles(listOf(ksA, ksB))
            val s2o = mapOf(
                fileA.relativeTo(baseDir) to setOf(outA.relativeTo(baseDir)),
            )
            ctx.updateCachesAndOutputs(dirty, setOf(outA), s2o)
            ctx.closeFiles()
        }

        // Predicate: isIncremental == false -> must return false
        run {
            val ctx = IncrementalContextAA(
                isIncremental = false,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            assertFalse(ctx.tryEarlyExitIfClean(listOf(srcRoot)), "Should NOT early-exit when isIncremental is false")
            ctx.closeFiles()
        }

        // Predicate: knownModified is non-empty -> must return false
        run {
            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = listOf(fileA),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            assertFalse(ctx.tryEarlyExitIfClean(listOf(srcRoot)), "Should NOT early-exit when knownModified is non-empty")
            ctx.closeFiles()
        }

        // Predicate: knownRemoved is non-empty -> must return false
        run {
            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = listOf(fileB),
                changedClasses = emptyList(),
            )
            assertFalse(ctx.tryEarlyExitIfClean(listOf(srcRoot)), "Should NOT early-exit when knownRemoved is non-empty")
            ctx.closeFiles()
        }

        // Positive case: wipe outputDir (simulating KspAAWorkerAction), run tryEarlyExitIfClean, verify output restoration & logs
        run {
            outputDir.deleteRecursively()
            assertFalse(outA.exists(), "outA should be deleted before early-exit runs")
            val symbolsCacheFile = File(cachesDir, "symbols")
            val sealedCacheFile = File(cachesDir, "sealed")
            val sourceToOutputsCacheFile = File(cachesDir, "sourceToOutputs")
            symbolsCacheFile.setLastModified(10000L)
            sealedCacheFile.setLastModified(10000L)
            sourceToOutputsCacheFile.setLastModified(10000L)

            val ctx = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = listOf("p1.Unrelated"),
            )
            assertTrue(ctx.tryEarlyExitIfClean(listOf(srcRoot)), "Should early-exit on clean incremental build")
            assertTrue(outA.exists(), "tryEarlyExitIfClean must restore clean outputs from backups when outputDir was wiped")
            assertEquals("package p1\nclass AGen", outA.readText())
            assertEquals(10000L, symbolsCacheFile.lastModified(), "Read-only early-exit must not rewrite symbols cache")
            assertEquals(10000L, sealedCacheFile.lastModified(), "Read-only early-exit must not rewrite sealed cache")
            assertEquals(10000L, sourceToOutputsCacheFile.lastModified(), "Read-only early-exit must not rewrite sourceToOutputs cache")
            val dirtyLog = File(cachesDir, "logs/kspDirtySet.log")
            assertTrue(dirtyLog.exists(), "kspDirtySet.log should be written when incrementalLoggingEnabled is true")
            assertTrue(dirtyLog.readText().contains("Dirty / All: 0.00%"))
        }

        // Predicate: unassociated output (Dependencies(false) with no originating sources) -> must return false
        val unassocCachesDir = File(tempDir, "caches_unassoc").apply { mkdirs() }
        val outUnassoc = File(outputDir, "kotlin/p1/UnassocGen.kt").apply {
            parentFile.mkdirs()
            writeText("package p1\nclass UnassocGen")
        }
        run {
            val ctx1 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = unassocCachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            val dirty1 = ctx1.calcDirtyFiles(listOf(ksA, ksB))
            // outUnassoc is in outputs, but not in sourceToOutputs (simulating Dependencies(false) with no sources)
            val s2o1 = mapOf(
                fileA.relativeTo(baseDir) to setOf(outA.relativeTo(baseDir)),
            )
            ctx1.updateCachesAndOutputs(dirty1, setOf(outA, outUnassoc), s2o1)
            ctx1.closeFiles()

            val ctx2 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = unassocCachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            assertFalse(
                ctx2.tryEarlyExitIfClean(listOf(srcRoot)),
                "Should NOT early-exit when prior build generated an unassociated output (Dependencies(false))"
            )
            ctx2.closeFiles()
        }

        // Predicate: aggregating output (anyChangesWildcard in sourceToOutputsMap) -> must return false
        run {
            val ctx1 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = listOf(fileA),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            val dirty = ctx1.calcDirtyFiles(listOf(ksA, ksB))
            val s2o = mapOf(
                anyChangesWildcard to setOf(outA.relativeTo(baseDir)),
            )
            ctx1.updateCachesAndOutputs(dirty, setOf(outA), s2o)
            ctx1.closeFiles()

            val ctx2 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = cachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            assertFalse(ctx2.tryEarlyExitIfClean(listOf(srcRoot)), "Should NOT early-exit when module has aggregating output")
            ctx2.closeFiles()
        }

        // Predicate: sealedMap is non-empty -> must return false
        val sealedCachesDir = File(tempDir, "caches_sealed").apply { mkdirs() }
        run {
            val ctx1 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = sealedCachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            val dirty = ctx1.calcDirtyFiles(listOf(ksA))
            ctx1.recordGetSealedSubclasses(ksA.declarations.first() as KSClassDeclaration)
            val s2o = mapOf(
                fileA.relativeTo(baseDir) to setOf(outA.relativeTo(baseDir)),
            )
            ctx1.updateCachesAndOutputs(dirty, setOf(outA), s2o)
            ctx1.closeFiles()

            val ctx2 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = sealedCachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            assertFalse(ctx2.tryEarlyExitIfClean(listOf(srcRoot)), "Should NOT early-exit when sealedMap is non-empty")
            ctx2.closeFiles()
        }

        // Predicate: removedOutputs is non-empty (a previous build reprocessed fileA and dropped outA) -> must return false
        val removedOutCachesDir = File(tempDir, "caches_removed_out").apply { mkdirs() }
        run {
            // Build 1: fileA generates outA
            val ctx1 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = removedOutCachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            val dirty1 = ctx1.calcDirtyFiles(listOf(ksA, ksB))
            ctx1.updateCachesAndOutputs(dirty1, setOf(outA), mapOf(fileA.relativeTo(baseDir) to setOf(outA.relativeTo(baseDir))))
            ctx1.closeFiles()

            // Build 2: fileA is modified and now generates NO outputs -> outA is recorded in removedOutputsKey
            val ctx2 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = removedOutCachesDir,
                kspOutputDir = outputDir,
                knownModified = listOf(fileA),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            val dirty2 = ctx2.calcDirtyFiles(listOf(ksA, ksB))
            ctx2.updateCachesAndOutputs(dirty2, emptySet(), emptyMap())
            ctx2.closeFiles()

            // Build 3: no modified/removed sources, but removedOutputsKey is non-empty -> must NOT early-exit
            val ctx3 = IncrementalContextAA(
                isIncremental = true,
                lookupTracker = DualLookupTracker(),
                anyChangesWildcard = anyChangesWildcard,
                loggingOptions = loggingOpts,
                baseDir = baseDir,
                cachesDir = removedOutCachesDir,
                kspOutputDir = outputDir,
                knownModified = emptyList(),
                knownRemoved = emptyList(),
                changedClasses = emptyList(),
            )
            assertFalse(ctx3.tryEarlyExitIfClean(listOf(srcRoot)), "Should NOT early-exit when removedOutputs is non-empty")
            ctx3.closeFiles()
        }
    }

    @Test
    fun testKotlinSymbolProcessingExecuteSkipsSessionAndProcessorOnCleanIncrementalRun(@TempDir tempDir: File) {
        val baseDir = File(tempDir, "project").apply { mkdirs() }
        val srcRoot = File(baseDir, "src/main/kotlin").apply { mkdirs() }
        val fileA = File(srcRoot, "p1/A.kt").apply {
            parentFile.mkdirs()
            writeText("package p1\nclass A\n")
        }
        val outputBaseDir = File(baseDir, "build/ksp").apply { mkdirs() }
        val classOutputDir = File(outputBaseDir, "classes").apply { mkdirs() }
        val javaOutputDir = File(outputBaseDir, "java").apply { mkdirs() }
        val kotlinOutputDir = File(outputBaseDir, "kotlin").apply { mkdirs() }
        val resourceOutputDir = File(outputBaseDir, "resources").apply { mkdirs() }
        val cachesDir = File(baseDir, "build/kspCaches").apply { mkdirs() }
        val jdkHomeFile = File(System.getProperty("java.home"))

        fun buildConfig(changedClassesList: List<String>): KSPJvmConfig = KSPJvmConfig.Builder().apply {
            moduleName = "testModule"
            sourceRoots = listOf(srcRoot)
            commonSourceRoots = emptyList()
            javaSourceRoots = emptyList()
            libraries = emptyList()
            friends = emptyList()
            jdkHome = jdkHomeFile
            jvmTarget = "17"
            languageVersion = "2.0"
            apiVersion = "2.0"
            projectBaseDir = baseDir
            this.outputBaseDir = outputBaseDir
            this.classOutputDir = classOutputDir
            this.javaOutputDir = javaOutputDir
            this.kotlinOutputDir = kotlinOutputDir
            this.resourceOutputDir = resourceOutputDir
            this.cachesDir = cachesDir
            incremental = true
            incrementalLog = true
            modifiedSources = emptyList()
            removedSources = emptyList()
            changedClasses = changedClassesList
        }.build()

        var build1ProviderCreateCount = 0
        val build1Provider = SymbolProcessorProvider { env: SymbolProcessorEnvironment ->
            build1ProviderCreateCount++
            object : SymbolProcessor {
                private var invoked = false
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (invoked) return emptyList()
                    invoked = true
                    val ksFiles = resolver.getAllFiles().toList()
                    env.codeGenerator.createNewFile(
                        Dependencies(aggregating = false, *ksFiles.toTypedArray()),
                        "p1",
                        "AGen"
                    ).use { out ->
                        out.write("package p1\nclass AGen\n".toByteArray())
                    }
                    return emptyList()
                }
            }
        }

        // Build 1: Initial incremental build runs AA session and processor, generating AGen.kt
        val exit1 = KotlinSymbolProcessing(
            buildConfig(emptyList()),
            listOf(build1Provider),
            CommandLineKSPLogger()
        ).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exit1)
        assertEquals(1, build1ProviderCreateCount, "Initial build must instantiate and run processor")
        val generatedFile = File(kotlinOutputDir, "p1/AGen.kt")
        assertTrue(generatedFile.exists(), "Generated file p1/AGen.kt must exist after Build 1")

        // Simulate KspAAWorkerAction wiping outputBaseDir before Build 2
        outputBaseDir.deleteRecursively()
        assertFalse(generatedFile.exists(), "Generated file should be wiped before Build 2")

        // Build 2: Incremental run with unrelated classpath change -> must early-exit without calling provider.create()
        var build2ProviderCreateCount = 0
        val build2Provider = SymbolProcessorProvider {
            build2ProviderCreateCount++
            throw AssertionError("SymbolProcessorProvider.create() must not be called when tryEarlyExitIfClean succeeds")
        }
        val exit2 = KotlinSymbolProcessing(
            buildConfig(listOf("other.pkg.UnrelatedClass")),
            listOf(build2Provider),
            CommandLineKSPLogger()
        ).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exit2)
        assertEquals(0, build2ProviderCreateCount, "Early-exit must skip processor instantiation")
        assertTrue(generatedFile.exists(), "Early-exit must restore generated file p1/AGen.kt from backups")
        assertEquals("package p1\nclass AGen\n", generatedFile.readText())
    }

    @Test
    fun testKotlinSymbolProcessingDoesNotLoseUnassociatedOutputsOnIncrementalRun(@TempDir tempDir: File) {
        val baseDir = File(tempDir, "project").apply { mkdirs() }
        val srcRoot = File(baseDir, "src/main/kotlin").apply { mkdirs() }
        File(srcRoot, "p1/A.kt").apply {
            parentFile.mkdirs()
            writeText("package p1\nclass A\n")
        }
        val outputBaseDir = File(baseDir, "build/ksp").apply { mkdirs() }
        val classOutputDir = File(outputBaseDir, "classes").apply { mkdirs() }
        val javaOutputDir = File(outputBaseDir, "java").apply { mkdirs() }
        val kotlinOutputDir = File(outputBaseDir, "kotlin").apply { mkdirs() }
        val resourceOutputDir = File(outputBaseDir, "resources").apply { mkdirs() }
        val cachesDir = File(baseDir, "build/kspCaches").apply { mkdirs() }
        val jdkHomeFile = File(System.getProperty("java.home"))

        fun buildConfig(changedClassesList: List<String>): KSPJvmConfig = KSPJvmConfig.Builder().apply {
            moduleName = "testModule"
            sourceRoots = listOf(srcRoot)
            commonSourceRoots = emptyList()
            javaSourceRoots = emptyList()
            libraries = emptyList()
            friends = emptyList()
            jdkHome = jdkHomeFile
            jvmTarget = "17"
            languageVersion = "2.0"
            apiVersion = "2.0"
            projectBaseDir = baseDir
            this.outputBaseDir = outputBaseDir
            this.classOutputDir = classOutputDir
            this.javaOutputDir = javaOutputDir
            this.kotlinOutputDir = kotlinOutputDir
            this.resourceOutputDir = resourceOutputDir
            this.cachesDir = cachesDir
            incremental = true
            incrementalLog = false
            modifiedSources = emptyList()
            removedSources = emptyList()
            changedClasses = changedClassesList
        }.build()

        var providerInvocations = 0
        val provider = SymbolProcessorProvider { env: SymbolProcessorEnvironment ->
            providerInvocations++
            object : SymbolProcessor {
                private var invoked = false
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (invoked) return emptyList()
                    invoked = true
                    env.codeGenerator.createNewFile(
                        Dependencies(aggregating = false),
                        "p1",
                        "UnassociatedGen"
                    ).use { out ->
                        out.write("package p1\nclass UnassociatedGen\n".toByteArray())
                    }
                    return emptyList()
                }
            }
        }

        // Build 1: Generates UnassociatedGen.kt with Dependencies(false) (zero originating sources)
        val exit1 = KotlinSymbolProcessing(
            buildConfig(emptyList()),
            listOf(provider),
            CommandLineKSPLogger()
        ).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exit1)
        assertEquals(1, providerInvocations)
        val unassocFile = File(kotlinOutputDir, "p1/UnassociatedGen.kt")
        assertTrue(unassocFile.exists(), "UnassociatedGen.kt must exist after Build 1")

        // Simulate KspAAWorkerAction wiping outputBaseDir before Build 2
        outputBaseDir.deleteRecursively()
        assertFalse(unassocFile.exists(), "UnassociatedGen.kt should be wiped before Build 2")

        // Build 2: Because Build 1 produced an unassociated output, tryEarlyExitIfClean must return false
        // so the processor runs and regenerates UnassociatedGen.kt rather than losing it after outputBaseDir wipe.
        val exit2 = KotlinSymbolProcessing(
            buildConfig(listOf("other.pkg.UnrelatedClass")),
            listOf(provider),
            CommandLineKSPLogger()
        ).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exit2)
        assertEquals(2, providerInvocations, "Processor must run when unassociated outputs exist")
        assertTrue(unassocFile.exists(), "UnassociatedGen.kt must exist after Build 2")
        assertEquals("package p1\nclass UnassociatedGen\n", unassocFile.readText())
    }
}
