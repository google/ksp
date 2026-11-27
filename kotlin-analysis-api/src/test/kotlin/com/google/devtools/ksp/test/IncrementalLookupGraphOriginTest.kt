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
import com.google.devtools.ksp.common.LookupSymbolWrapper
import com.google.devtools.ksp.common.NoSourceFile
import com.google.devtools.ksp.common.ROOT_PACKAGE_SCOPE
import com.google.devtools.ksp.common.impl.KSNameImpl
import com.google.devtools.ksp.common.normalizeLookupScope
import com.google.devtools.ksp.common.toGraphVertexName
import com.google.devtools.ksp.common.toLookupSymbolWrapper
import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.DualLookupTracker
import com.google.devtools.ksp.impl.IncrementalContextAA
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.impl.symbol.kotlin.separateQualifierAndName
import com.google.devtools.ksp.impl.toLookupScopeAndName
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IncrementalLookupGraphOriginTest {
    @TempDir
    lateinit var tmp: File

    private val base by lazy { File(tmp, "project") }
    private val caches by lazy { File(tmp, "caches") }
    private val out by lazy { File(tmp, "out") }
    private val dotFile by lazy { File(caches, "logs/kspLookupGraphOrigin.dot") }

    @Test
    fun `normalizes root-package source, target, and NoSourceFile vertices without anonymous prefix`() {
        val rootSrc = src("RootConsumer.kt")
        val ksFile = FakeKSFile(rootSrc, "", listOf(FakeClassDeclaration("RootConsumer", "")))
        val binarySourcePath = NoSourceFile(base, "RootTarget").filePath

        runBuildWithGraphOrigin(
            origin = "RootConsumer",
            ksFiles = listOf(ksFile),
        ) { tracker ->
            tracker.record(rootSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, "RootTarget")
            tracker.record(binarySourcePath, Position.NO_POSITION, "p1", ScopeKind.PACKAGE, "PackagedLeaf")
        }

        assertEquals(
            """
            digraph {
              "RootConsumer";
              "RootConsumer" -> "RootTarget";
              "RootTarget" -> "p1.PackagedLeaf";
            }
            """.trimIndent(),
            dotFile.readText().trim()
        )
    }

    @Test
    fun `preserves leading dot for backticked root-package symbol and avoids collision with non-dotted symbol`() {
        val dotFooSrc = src("DotFoo.kt")
        val fooSrc = src("Foo.kt")
        val dotFooFile = FakeKSFile(dotFooSrc, "", listOf(FakeClassDeclaration(".foo", "")))
        val fooFile = FakeKSFile(fooSrc, "", listOf(FakeClassDeclaration("foo", "")))

        runBuildWithGraphOrigin(
            origin = ".foo",
            ksFiles = listOf(dotFooFile, fooFile),
        ) { tracker ->
            tracker.record(dotFooSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, "FromDotFoo")
            tracker.record(fooSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, "FromPlainFoo")
            tracker.record(fooSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, ".foo")
        }

        assertEquals(
            """
            digraph {
              ".foo";
              ".foo" -> "FromDotFoo";
            }
            """.trimIndent(),
            dotFile.readText().trim()
        )

        // Querying with `<anonymous>..foo` must also strip only `<anonymous>.` and keep `.foo`.
        runBuildWithGraphOrigin(
            origin = "$ROOT_PACKAGE_SCOPE..foo",
            ksFiles = listOf(dotFooFile, fooFile),
        ) { tracker ->
            tracker.record(dotFooSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, "FromDotFoo")
            tracker.record(fooSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, "FromPlainFoo")
        }

        assertEquals(
            """
            digraph {
              ".foo";
              ".foo" -> "FromDotFoo";
            }
            """.trimIndent(),
            dotFile.readText().trim()
        )

        // Also verify querying from plain "foo" traverses into ".foo" without collapsing ".foo" to a self-loop on "foo".
        runBuildWithGraphOrigin(
            origin = "foo",
            ksFiles = listOf(dotFooFile, fooFile),
        ) { tracker ->
            tracker.record(dotFooSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, "FromDotFoo")
            tracker.record(fooSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, "FromPlainFoo")
            tracker.record(fooSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, ".foo")
        }

        assertEquals(
            """
            digraph {
              "foo";
              ".foo" -> "FromDotFoo";
              "foo" -> ".foo";
              "foo" -> "FromPlainFoo";
            }
            """.trimIndent(),
            dotFile.readText().trim()
        )
    }

    @Test
    fun `extracts normalized LookupSymbolWrapper and vertex names across root, packaged, nested, and backticked symbols`() {
        assertEquals(ROOT_PACKAGE_SCOPE, "".normalizeLookupScope())
        assertEquals("p1.p2", "p1.p2".normalizeLookupScope())
        assertEquals(ROOT_PACKAGE_SCOPE to "RootClass", separateQualifierAndName("RootClass"))
        assertEquals(ROOT_PACKAGE_SCOPE to ".foo", separateQualifierAndName(".foo"))
        assertEquals(ROOT_PACKAGE_SCOPE to "..foo", separateQualifierAndName("..foo"))
        assertEquals(".Outer" to "Inner", separateQualifierAndName(".Outer.Inner"))
        assertEquals("Outer." to "Inner", separateQualifierAndName("Outer..Inner"))
        assertEquals("p1.p2" to "Leaf", separateQualifierAndName("p1.p2.Leaf"))

        assertEquals(
            LookupSymbolWrapper("RootClass", ROOT_PACKAGE_SCOPE),
            FakeClassDeclaration("RootClass", "").toLookupSymbolWrapper()
        )
        assertEquals(
            LookupSymbolWrapper("dotted.target", "com.example.downstream"),
            FakeClassDeclaration("dotted.target", "com.example.downstream").toLookupSymbolWrapper()
        )
        assertEquals(
            LookupSymbolWrapper(".foo", ROOT_PACKAGE_SCOPE),
            FakeClassDeclaration(".foo", "").toLookupSymbolWrapper()
        )
        assertEquals(
            LookupSymbolWrapper("Inner", "Outer."),
            FakeClassDeclaration("Inner", "", qualified = "Outer..Inner").toLookupSymbolWrapper()
        )

        assertEquals("RootClass", LookupSymbolWrapper("RootClass", ROOT_PACKAGE_SCOPE).toGraphVertexName())
        assertEquals("RootClass", LookupSymbolWrapper("RootClass", "").toGraphVertexName())
        assertEquals(".foo", LookupSymbolWrapper(".foo", ROOT_PACKAGE_SCOPE).toGraphVertexName())
        assertEquals("p1.Leaf", LookupSymbolWrapper("Leaf", "p1").toGraphVertexName())

        assertEquals(
            ROOT_PACKAGE_SCOPE to "RootClass",
            ClassId(FqName.ROOT, Name.identifier("RootClass")).toLookupScopeAndName()
        )
        assertEquals(
            ROOT_PACKAGE_SCOPE to ".foo",
            ClassId(FqName.ROOT, Name.identifier(".foo")).toLookupScopeAndName()
        )
        assertEquals(
            "p" to "dotted.target",
            ClassId(FqName("p"), Name.identifier("dotted.target")).toLookupScopeAndName()
        )
        assertEquals(
            "Outer" to "Inner",
            ClassId(FqName.ROOT, FqName("Outer.Inner"), isLocal = false).toLookupScopeAndName()
        )
    }

    @Test
    fun `dumpLookupRecords and kspLookupGraph preserve both entries when dotted symbol names collide`() {
        val consumerA = src("ConsumerA.kt")
        val consumerB = src("ConsumerB.kt")
        val tracker = DualLookupTracker()
        val context = IncrementalContextAA(
            isIncremental = true,
            lookupTracker = tracker,
            anyChangesWildcard = File("<any>"),
            loggingOptions = IncrementalContextLoggingOptions(
                incrementalLoggingEnabled = true,
                dependencyGraphOriginName = null,
            ),
            baseDir = base,
            cachesDir = caches,
            kspOutputDir = out,
            knownModified = emptyList(),
            knownRemoved = emptyList(),
            changedClasses = emptyList(),
        )
        val ksFiles = listOf(
            FakeKSFile(consumerA, "", listOf(FakeClassDeclaration("ConsumerA", ""))),
            FakeKSFile(consumerB, "", listOf(FakeClassDeclaration("ConsumerB", ""))),
        )
        val dirty = context.calcDirtyFiles(ksFiles)
        tracker.record(consumerA.path, Position.NO_POSITION, "p.dotted", ScopeKind.PACKAGE, "target")
        tracker.record(consumerB.path, Position.NO_POSITION, "p", ScopeKind.PACKAGE, "dotted.target")

        val dumped = context.dumpLookupRecords()
        assertEquals(
            setOf(consumerA.relativeTo(base).path, consumerB.relativeTo(base).path),
            dumped["p.dotted.target"]?.toSet()
        )

        context.updateCachesAndOutputs(dirty, emptySet(), emptyMap())
        context.closeFiles()

        val logLines = File(caches, "logs/kspLookupGraph.log").readLines()
        assertEquals(
            setOf(
                "${consumerA.relativeTo(base).path} -> p.dotted.target",
                "${consumerB.relativeTo(base).path} -> p.dotted.target",
            ),
            logLines.filter { "->" in it }.toSet()
        )
    }

    @Test
    fun `invalidates Kotlin consumer when root-package classpath class changes`() {
        val consumerSrc = src("Consumer.kt")
        val consumerFile = FakeKSFile(consumerSrc, "", listOf(FakeClassDeclaration("Consumer", "")))

        // Build 1: Kotlin consumer looks up root-package classpath symbol (DualLookupTracker receives scopeFqName = "").
        caches.deleteRecursively()
        run {
            val tracker = DualLookupTracker()
            val context = newIncrementalContext(tracker = tracker)
            val dirty = context.calcDirtyFiles(listOf(consumerFile))
            tracker.record(consumerSrc.path, Position.NO_POSITION, "", ScopeKind.PACKAGE, "RootClasspathTarget")
            tracker.record(
                consumerSrc.path,
                Position.NO_POSITION,
                "RootClasspathTarget",
                ScopeKind.CLASSIFIER,
                "member"
            )
            context.updateCachesAndOutputs(dirty, emptySet(), emptyMap())
            context.closeFiles()
        }

        // Build 2: RootClasspathTarget changes on classpath -> must NOT early-exit and must mark Consumer.kt dirty.
        run {
            val earlyExitContext = newIncrementalContext(changedClasses = listOf("RootClasspathTarget"))
            assertFalse(earlyExitContext.tryEarlyExitIfClean { listOf(consumerSrc) })
            earlyExitContext.closeFiles()

            val context = newIncrementalContext(changedClasses = listOf("RootClasspathTarget"))
            val dirty = context.calcDirtyFiles(listOf(consumerFile))
            assertEquals(listOf(consumerSrc.path), dirty.map { it.filePath })
            context.closeFiles()
        }
    }

    @Test
    fun `invalidates consumer recorded via LookupTrackerWrapperImpl when root-package declaration is modified`() {
        val javaConsumerSrc = src("JavaConsumer.java")
        val rootTargetSrc = src("RootTarget.kt")
        val javaConsumerFile = FakeKSFile(javaConsumerSrc, "", listOf(FakeClassDeclaration("JavaConsumer", "")))
        val rootTargetFile = FakeKSFile(rootTargetSrc, "", listOf(FakeClassDeclaration("RootTarget", "")))

        // Build 1: Java consumer records lookup with empty/root scope via LookupTrackerWrapperImpl (as recordLookup does).
        caches.deleteRecursively()
        run {
            val tracker = DualLookupTracker()
            val context = newIncrementalContext(tracker = tracker)
            val dirty = context.calcDirtyFiles(listOf(javaConsumerFile, rootTargetFile))
            context.recordClassReferenceLookup("RootTarget", FakeClassDeclaration("JavaConsumer", "", containing = javaConsumerFile))
            context.updateCachesAndOutputs(dirty, emptySet(), emptyMap())
            context.closeFiles()
        }

        // Build 2: RootTarget.kt is modified -> SymbolCollector + DirtinessPropagator must invalidate JavaConsumer.java.
        run {
            val context = newIncrementalContext(knownModified = listOf(rootTargetSrc))
            val dirty = context.calcDirtyFiles(listOf(javaConsumerFile, rootTargetFile))
            assertEquals(
                setOf(javaConsumerSrc.path, rootTargetSrc.path),
                dirty.map { it.filePath }.toSet()
            )
            context.closeFiles()
        }
    }

    @Test
    fun `records lossless ClassId scope and name for backticked dotted and leading-dot class literal arguments`() {
        val srcRoot = File(base, "src/main")
        File(srcRoot, "java/Anno.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                @Retention(RetentionPolicy.RUNTIME)
                public @interface Anno {
                    Class<?> value();
                }
                """.trimIndent()
            )
        }
        val dotFooKt = File(srcRoot, "kotlin/DotFoo.kt").apply {
            parentFile.mkdirs()
            writeText("class `.foo`\n")
        }
        val plainFooKt = File(srcRoot, "kotlin/PlainFoo.kt").apply {
            writeText("class foo\n")
        }
        val dottedTargetKt = File(srcRoot, "kotlin/DottedTarget.kt").apply {
            writeText("package p\nclass `dotted.target`\n")
        }
        File(srcRoot, "kotlin/DotConsumer.kt").apply {
            writeText("@Anno(`.foo`::class)\nclass DotConsumer\n")
        }
        File(srcRoot, "kotlin/PackagedDottedConsumer.kt").apply {
            writeText("@Anno(p.`dotted.target`::class)\nclass PackagedDottedConsumer\n")
        }

        val processed = mutableListOf<String>()
        val provider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    resolver.getSymbolsWithAnnotation("Anno")
                        .filterIsInstance<KSClassDeclaration>()
                        .forEach { decl ->
                            processed.add(decl.simpleName.asString())
                            decl.annotations.forEach { anno ->
                                anno.arguments.forEach { arg ->
                                    (arg.value as? KSType)?.declaration?.simpleName?.asString()
                                }
                            }
                        }
                    return emptyList()
                }
            }
        }

        fun executeKsp(modified: List<File> = emptyList(), graphOrigin: String? = null) {
            processed.clear()
            val config = KSPJvmConfig.Builder().apply {
                moduleName = "test"
                sourceRoots = listOf(File(srcRoot, "kotlin"))
                javaSourceRoots = listOf(File(srcRoot, "java"))
                commonSourceRoots = emptyList()
                libraries = emptyList()
                friends = emptyList()
                jdkHome = File(System.getProperty("java.home"))
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
                incrementalLog = true
                incrementalLogGraphOrigin = graphOrigin
                modifiedSources = modified
                removedSources = emptyList()
                changedClasses = emptyList()
            }.build()
            assertEquals(
                KotlinSymbolProcessing.ExitCode.OK,
                KotlinSymbolProcessing(config, listOf(provider), CommandLineKSPLogger()).execute()
            )
        }

        // 1. Clean build: verify `.dot` graph from DotConsumer points to `.foo` (not `foo`).
        executeKsp(graphOrigin = "DotConsumer")
        assertEquals(setOf("DotConsumer", "PackagedDottedConsumer"), processed.toSet())
        val dotContent = dotFile.readText()
        assertEquals(
            true,
            dotContent.contains("\"DotConsumer\" -> \".foo\";"),
            "Expected DotConsumer -> .foo edge, got:\n$dotContent"
        )
        assertFalse(
            dotContent.contains("\"DotConsumer\" -> \"foo\";"),
            "Unexpected collision with plain foo in:\n$dotContent"
        )

        // 2. Modifying plain `foo` must NOT falsely invalidate `DotConsumer`.
        plainFooKt.writeText("class foo { fun added() {} }\n")
        executeKsp(modified = listOf(plainFooKt))
        assertEquals(emptyList<String>(), processed)

        // 3. Modifying `p.dotted.target` must invalidate ONLY `PackagedDottedConsumer`.
        dottedTargetKt.writeText("package p\nclass `dotted.target` { fun added() {} }\n")
        executeKsp(modified = listOf(dottedTargetKt))
        assertEquals(listOf("PackagedDottedConsumer"), processed)

        // 4. Modifying `.foo` must invalidate ONLY `DotConsumer`.
        dotFooKt.writeText("class `.foo` { fun added() {} }\n")
        executeKsp(modified = listOf(dotFooKt))
        assertEquals(listOf("DotConsumer"), processed)
    }

    private fun src(path: String) = File(base, "src/main/kotlin/$path").apply {
        parentFile.mkdirs()
        writeText("")
    }

    private fun runBuildWithGraphOrigin(
        origin: String,
        ksFiles: List<KSFile>,
        recordLookups: (DualLookupTracker) -> Unit,
    ) {
        caches.deleteRecursively()
        val tracker = DualLookupTracker()
        val context = IncrementalContextAA(
            isIncremental = true,
            lookupTracker = tracker,
            anyChangesWildcard = File("<any>"),
            loggingOptions = IncrementalContextLoggingOptions(
                incrementalLoggingEnabled = true,
                dependencyGraphOriginName = origin,
            ),
            baseDir = base,
            cachesDir = caches,
            kspOutputDir = out,
            knownModified = emptyList(),
            knownRemoved = emptyList(),
            changedClasses = emptyList(),
        )
        val dirty = context.calcDirtyFiles(ksFiles)
        recordLookups(tracker)
        context.updateCachesAndOutputs(dirty, emptySet(), emptyMap())
        context.closeFiles()
    }

    private fun newIncrementalContext(
        tracker: DualLookupTracker = DualLookupTracker(),
        knownModified: List<File> = emptyList(),
        changedClasses: List<String> = emptyList(),
    ) = IncrementalContextAA(
        isIncremental = true,
        lookupTracker = tracker,
        anyChangesWildcard = File("<any>"),
        loggingOptions = IncrementalContextLoggingOptions(false, null),
        baseDir = base,
        cachesDir = caches,
        kspOutputDir = out,
        knownModified = knownModified,
        knownRemoved = emptyList(),
        changedClasses = changedClasses,
    )

    private class FakeKSFile(
        file: File,
        pkg: String,
        private val decls: List<KSDeclaration>,
    ) : KSFile {
        override val filePath: String = file.path
        override val fileName: String = file.name
        override val packageName: KSName = KSNameImpl.getCached(pkg)
        override val declarations: Sequence<KSDeclaration> = decls.asSequence()
        override val annotations: Sequence<KSAnnotation> = emptySequence()
        override val origin: Origin = Origin.KOTLIN
        override val location: Location = FileLocation(filePath, 1)
        override val parent: KSNode? = null
        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = visitor.visitFile(this, data)
    }

    private class FakeClassDeclaration(
        simple: String,
        pkg: String,
        qualified: String = if (pkg.isEmpty()) simple else "$pkg.$simple",
        containing: KSFile? = null,
    ) : KSClassDeclaration {
        override val simpleName: KSName = KSNameImpl.getCached(simple)
        override val packageName: KSName = KSNameImpl.getCached(pkg)
        override val qualifiedName: KSName = KSNameImpl.getCached(qualified)
        override val classKind: ClassKind = ClassKind.CLASS
        override val primaryConstructor: KSFunctionDeclaration? = null
        override val superTypes: Sequence<KSTypeReference> = emptySequence()
        override val isCompanionObject: Boolean = false
        override val declarations: Sequence<KSDeclaration> = emptySequence()
        override val typeParameters: List<KSTypeParameter> = emptyList()
        override val parentDeclaration: KSDeclaration? = null
        override val containingFile: KSFile? = containing
        override val docString: String? = null
        override val modifiers: Set<Modifier> = emptySet()
        override val origin: Origin = Origin.KOTLIN
        override val location: Location = FileLocation("", 1)
        override val parent: KSNode? = null
        override val annotations: Sequence<KSAnnotation> = emptySequence()
        override val isActual: Boolean = false
        override val isExpect: Boolean = false
        override fun findActuals(): Sequence<KSDeclaration> = emptySequence()
        override fun findExpects(): Sequence<KSDeclaration> = emptySequence()
        override fun getSealedSubclasses(): Sequence<KSClassDeclaration> = emptySequence()
        override fun getAllFunctions(): Sequence<KSFunctionDeclaration> = emptySequence()
        override fun getAllProperties(): Sequence<KSPropertyDeclaration> = emptySequence()
        override fun asType(typeArguments: List<KSTypeArgument>): KSType = throw UnsupportedOperationException()
        override fun asStarProjectedType(): KSType = throw UnsupportedOperationException()
        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R = visitor.visitClassDeclaration(this, data)
    }
}
