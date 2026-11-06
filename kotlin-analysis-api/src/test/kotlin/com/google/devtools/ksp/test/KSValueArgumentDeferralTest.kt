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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.symbol.kotlin.Deferrable
import com.google.devtools.ksp.impl.symbol.kotlin.DeferrableAnnotation
import com.google.devtools.ksp.impl.symbol.kotlin.KSErrorType
import com.google.devtools.ksp.impl.symbol.kotlin.Restorable
import com.google.devtools.ksp.impl.symbol.kotlin.toValue
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.annotations.KaNamedAnnotationValue
import org.jetbrains.kotlin.analysis.api.platform.lifetime.KotlinAlwaysAccessibleLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KSValueArgumentDeferralTest {

    @Test
    fun testDeferredValueArgumentsPreserveParentAndRecordLookupsAA(@TempDir tempDir: File) {
        runDeferredValueArgumentsTest(experimentalPsiResolution = false, tempDir = tempDir)
    }

    @Test
    fun testDeferredValueArgumentsPreserveParentAndRecordLookupsPsi(@TempDir tempDir: File) {
        runDeferredValueArgumentsTest(experimentalPsiResolution = true, tempDir = tempDir)
    }

    private fun runDeferredValueArgumentsTest(
        experimentalPsiResolution: Boolean,
        tempDir: File,
    ) {
        val kotlinSrc = File(tempDir, "kotlinSrc").apply { mkdirs() }
        val javaSrc = File(tempDir, "javaSrc").apply { mkdirs() }

        File(kotlinSrc, "AnnotationsAndTargets.kt").writeText(
            """
            import kotlin.reflect.KClass

            class TargetKotlinExplicit
            class TargetKotlinDefault
            class TargetJavaExplicit
            class TargetJavaDefault

            annotation class KotlinRef(val value: KClass<*>)
            annotation class KotlinDefaultRef(val value: KClass<*> = TargetKotlinDefault::class)
            """.trimIndent() + "\n"
        )
        File(kotlinSrc, "KotlinExplicitConsumer.kt").writeText(
            """
            @KotlinRef(TargetKotlinExplicit::class)
            class KotlinExplicitConsumer
            """.trimIndent() + "\n"
        )
        File(kotlinSrc, "KotlinDefaultConsumer.kt").writeText(
            """
            @KotlinDefaultRef
            class KotlinDefaultConsumer
            """.trimIndent() + "\n"
        )
        File(javaSrc, "JavaRef.java").writeText(
            """
            public @interface JavaRef {
                Class<?> value();
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "JavaDefaultRef.java").writeText(
            """
            public @interface JavaDefaultRef {
                Class<?> value() default TargetJavaDefault.class;
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "JavaConsumer.java").writeText(
            """
            @JavaRef(TargetJavaExplicit.class)
            @JavaDefaultRef
            public class JavaConsumer {
            }
            """.trimIndent() + "\n"
        )

        var roundCount = 0
        var round2Lookups: Map<String, List<String>> = emptyMap()
        val restoredSummaries = mutableListOf<String>()

        val provider = SymbolProcessorProvider { env: SymbolProcessorEnvironment ->
            object : SymbolProcessor {
                private val deferredArgs = mutableMapOf<String, Restorable?>()

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (roundCount++ == 0) {
                        val ktExplicitArg = resolver.getClassDeclarationByName("KotlinExplicitConsumer")!!
                            .annotations.single().arguments.single()
                        val ktDefaultArg = resolver.getClassDeclarationByName("KotlinDefaultConsumer")!!
                            .annotations.single().arguments.single()
                        val javaAnnos = resolver.getClassDeclarationByName("JavaConsumer")!!.annotations.toList()
                        val javaExplicitArg = javaAnnos.single { it.shortName.asString() == "JavaRef" }
                            .arguments.single()
                        val javaDefaultArg = javaAnnos.single { it.shortName.asString() == "JavaDefaultRef" }
                            .arguments.single()

                        deferredArgs["ktExplicit"] = (ktExplicitArg as? Deferrable)?.defer()
                        deferredArgs["ktDefault"] = (ktDefaultArg as? Deferrable)?.defer()
                        deferredArgs["javaExplicit"] = (javaExplicitArg as? Deferrable)?.defer()
                        deferredArgs["javaDefault"] = (javaDefaultArg as? Deferrable)?.defer()

                        // Confirm none of the synthetic/Java targets were recorded yet in Round 1 before .value read
                        val round1Lookups = ResolverAAImpl.instance.incrementalContext.dumpLookupRecords()
                        assertTrue("kotlinSrc/KotlinDefaultConsumer.kt" !in (round1Lookups["<anonymous>.TargetKotlinDefault"] ?: emptyList()))
                        assertTrue("javaSrc/JavaConsumer.java" !in (round1Lookups["<anonymous>.TargetJavaExplicit"] ?: emptyList()))
                        assertTrue("javaSrc/JavaConsumer.java" !in (round1Lookups["<anonymous>.TargetJavaDefault"] ?: emptyList()))

                        env.codeGenerator.createNewFile(Dependencies(false), "", "Round2Trigger", "kt").use {
                            it.write("class Round2Trigger\n".toByteArray())
                        }
                    } else if (roundCount == 2) {
                        for ((key, restorable) in deferredArgs) {
                            assertNotNull(restorable, "Expected Deferrable.defer() to be non-null for $key")
                            val restored = restorable!!.restore() as? KSValueArgument
                            assertNotNull(restored, "Expected Restorable.restore() to return KSValueArgument for $key")
                            val parentAnno = restored!!.parent as? KSAnnotation
                            assertNotNull(parentAnno, "Expected restored KSValueArgument.parent to be KSAnnotation for $key")
                            val ownerDecl = parentAnno!!.parent as? KSClassDeclaration
                            assertNotNull(ownerDecl, "Expected parent annotation's parent to be KSClassDeclaration for $key")
                            val typeName = (restored.value as KSType).declaration.simpleName.asString()
                            restoredSummaries.add("$key:${ownerDecl!!.simpleName.asString()}->$typeName")
                        }
                        round2Lookups = ResolverAAImpl.instance.incrementalContext.dumpLookupRecords()
                    }
                    return emptyList()
                }
            }
        }

        val kspConfig = KSPJvmConfig.Builder().apply {
            moduleName = "main"
            sourceRoots = listOf(kotlinSrc)
            javaSourceRoots = listOf(javaSrc)
            jdkHome = File(System.getProperty("java.home"))
            jvmTarget = JvmTarget.DEFAULT.description
            languageVersion = "2.0"
            apiVersion = "2.0"
            libraries = listOf(File("dist/kotlinc/lib/kotlin-stdlib.jar"))
            projectBaseDir = tempDir
            classOutputDir = File(tempDir, "out/classes")
            javaOutputDir = File(tempDir, "out/java")
            kotlinOutputDir = File(tempDir, "out/kotlin")
            resourceOutputDir = File(tempDir, "out/resources")
            cachesDir = File(tempDir, "out/caches")
            outputBaseDir = File(tempDir, "out")
            incremental = true
            this.experimentalPsiResolution = experimentalPsiResolution
        }.build()

        val exitCode = KotlinSymbolProcessing(kspConfig, listOf(provider), CommandLineKSPLogger()).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exitCode)
        assertEquals(2, roundCount)
        assertEquals(
            listOf(
                "ktExplicit:KotlinExplicitConsumer->TargetKotlinExplicit",
                "ktDefault:KotlinDefaultConsumer->TargetKotlinDefault",
                "javaExplicit:JavaConsumer->TargetJavaExplicit",
                "javaDefault:JavaConsumer->TargetJavaDefault",
            ),
            restoredSummaries
        )
        assertTrue("kotlinSrc/KotlinExplicitConsumer.kt" in (round2Lookups["<anonymous>.TargetKotlinExplicit"] ?: emptyList()))
        assertTrue("kotlinSrc/KotlinDefaultConsumer.kt" in (round2Lookups["<anonymous>.TargetKotlinDefault"] ?: emptyList()))
        assertTrue("javaSrc/JavaConsumer.java" in (round2Lookups["<anonymous>.TargetJavaExplicit"] ?: emptyList()))
        assertTrue("javaSrc/JavaConsumer.java" in (round2Lookups["<anonymous>.TargetJavaDefault"] ?: emptyList()))
    }

    @Test
    fun testDeferredDisambiguationAndJavaEdgeCasesAA(@TempDir tempDir: File) {
        runDeferredDisambiguationAndJavaEdgeCasesTest(experimentalPsiResolution = false, tempDir = tempDir)
    }

    @Test
    fun testDeferredDisambiguationAndJavaEdgeCasesPsi(@TempDir tempDir: File) {
        runDeferredDisambiguationAndJavaEdgeCasesTest(experimentalPsiResolution = true, tempDir = tempDir)
    }

    @OptIn(KspExperimental::class)
    private fun runDeferredDisambiguationAndJavaEdgeCasesTest(
        experimentalPsiResolution: Boolean,
        tempDir: File,
    ) {
        val kotlinSrc = File(tempDir, "kotlinSrc").apply { mkdirs() }
        val javaSrc = File(tempDir, "javaSrc").apply { mkdirs() }

        File(kotlinSrc, "Pkg1Dup.kt").writeText(
            """
            package pkg1
            import kotlin.reflect.KClass
            annotation class DupNameRef(val value: KClass<*>)
            """.trimIndent() + "\n"
        )
        File(kotlinSrc, "Pkg2Dup.kt").writeText(
            """
            package pkg2
            import kotlin.reflect.KClass
            annotation class DupNameRef(val value: KClass<*>)
            """.trimIndent() + "\n"
        )
        File(kotlinSrc, "DisambiguationKotlin.kt").writeText(
            """
            import kotlin.reflect.KClass
            import pkg.GeneratedPkgKtRef as ImportAliasedKtRef

            typealias TypeAliasedKtRef = pkg.GeneratedPkgKtRef

            class TargetFirst
            class TargetSecond
            class TargetValidBesideUnresolved
            class TargetGeneratedJavaPositional
            class TargetJavaNestedExplicit
            class TargetJavaNestedDefault

            annotation class InnerRef(val value: KClass<*>)
            annotation class OuterArray(val items: Array<InnerRef>)

            @Repeatable
            annotation class RepeatableTag(val value: KClass<*>)

            @OuterArray([InnerRef(TargetFirst::class), InnerRef(TargetSecond::class)])
            class DisambiguationHolder {
                @RepeatableTag(TargetFirst::class)
                @RepeatableTag(TargetSecond::class)
                val repeatableProp: Int = 0

                @pkg1.DupNameRef(TargetFirst::class)
                @pkg2.DupNameRef(TargetSecond::class)
                val dupNameProp: Int = 0

                @GeneratedKtRef
                @InnerRef(TargetValidBesideUnresolved::class)
                val unresolvedSiblingProp: Int = 0

                @pkg.GeneratedPkgKtRef
                val unresolvedQualifiedProp: Int = 0

                @ImportAliasedKtRef(pkg.PkgTargetSecond::class)
                val importAliasedProp: Int = 0

                @TypeAliasedKtRef(pkg.PkgTargetSecond::class)
                val typeAliasedProp: Int = 0
            }

            typealias TypeParamAnno<T> = T
            class GenericUnresolvedAnnoHolder<T> {
                @TypeParamAnno<T>
                val nullClassIdProp: Int = 0
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "JavaInnerRef.java").writeText(
            """
            public @interface JavaInnerRef {
                Class<?> value();
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "JavaOuterArray.java").writeText(
            """
            public @interface JavaOuterArray {
                JavaInnerRef[] value();
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "JavaOuterDefault.java").writeText(
            """
            public @interface JavaOuterDefault {
                JavaInnerRef value() default @JavaInnerRef(TargetJavaNestedDefault.class);
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "JavaDisambiguationConsumer.java").writeText(
            """
            @JavaOuterArray({@JavaInnerRef(TargetFirst.class), @JavaInnerRef(TargetJavaNestedExplicit.class)})
            @JavaOuterDefault
            @GeneratedJavaRef(TargetGeneratedJavaPositional.class)
            public class JavaDisambiguationConsumer {
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "pkg").mkdirs()
        File(javaSrc, "pkg/PkgTargetFirst.java").writeText("package pkg;\npublic class PkgTargetFirst {}\n")
        File(javaSrc, "pkg/PkgTargetSecond.java").writeText("package pkg;\npublic class PkgTargetSecond {}\n")
        File(javaSrc, "pkg/PkgInfoResolvedRef.java").writeText(
            """
            package pkg;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Target;

            @Target(ElementType.PACKAGE)
            public @interface PkgInfoResolvedRef {
                Class<?> value();
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "pkg/package-info.java").writeText(
            """
            @PkgInfoResolvedRef(PkgTargetFirst.class)
            @GeneratedPkgInfoAnno(PkgTargetSecond.class)
            package pkg;
            """.trimIndent() + "\n"
        )
        File(javaSrc, "pkg/JavaOuterPkgNested.java").writeText(
            """
            package pkg;

            public @interface JavaOuterPkgNested {
                GeneratedJavaNested value();
                GeneratedJavaNested[] items();
            }
            """.trimIndent() + "\n"
        )
        File(javaSrc, "pkg/JavaPkgNestedConsumer.java").writeText(
            """
            package pkg;

            @JavaOuterPkgNested(
                value = @GeneratedJavaNested(PkgTargetFirst.class),
                items = {@GeneratedJavaNested(PkgTargetSecond.class)}
            )
            public class JavaPkgNestedConsumer {
                public void consume(
                    @GeneratedJavaNested(PkgTargetFirst.class)
                    @JavaOuterPkgNested(
                        value = @GeneratedJavaNested(PkgTargetFirst.class),
                        items = {@GeneratedJavaNested(PkgTargetSecond.class)}
                    ) String param
                ) {}
            }
            """.trimIndent() + "\n"
        )

        var roundCount = 0
        var round1Lookups: Map<String, List<String>> = emptyMap()
        var round2Lookups: Map<String, List<String>> = emptyMap()
        var restoredUnresolvedKtAnnoTypeName: String? = null
        val restoredNestedAnnotationFqNames = mutableListOf<String?>()
        val restoredSummaries = mutableListOf<String>()

        val provider = SymbolProcessorProvider { env: SymbolProcessorEnvironment ->
            object : SymbolProcessor {
                private val deferredArgs = linkedMapOf<String, Restorable?>()
                private var deferredUnresolvedKtAnno: (() -> KSAnnotation?)? = null
                private val deferredNestedAnnos = linkedMapOf<String, (() -> KSAnnotation?)?>()
                private val deferredNestedContainerArgs = linkedMapOf<String, Restorable?>()

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (roundCount++ == 0) {
                        val symbolsWithInnerRef = resolver.getSymbolsWithAnnotation("InnerRef").toList()
                        assertTrue(symbolsWithInnerRef.any { (it as? KSPropertyDeclaration)?.simpleName?.asString() == "unresolvedSiblingProp" })

                        val ktHolder = resolver.getClassDeclarationByName("DisambiguationHolder")!!
                        val outerArrayList = ktHolder.annotations.single().arguments.single().value as List<*>
                        val ktArraySecondArg = (outerArrayList[1] as KSAnnotation).arguments.single()

                        val repeatableProp = ktHolder.declarations.single {
                            it.simpleName.asString() == "repeatableProp"
                        } as KSPropertyDeclaration
                        val ktRepeatableSecondArg = repeatableProp.annotations.toList()[1].arguments.single()

                        val dupNameProp = ktHolder.declarations.single {
                            it.simpleName.asString() == "dupNameProp"
                        } as KSPropertyDeclaration
                        val ktDupSecondArg = dupNameProp.annotations.toList()[1].arguments.single()

                        val unresolvedSiblingProp = ktHolder.declarations.single {
                            it.simpleName.asString() == "unresolvedSiblingProp"
                        } as KSPropertyDeclaration
                        val unresolvedSiblingAnnos = unresolvedSiblingProp.annotations.toList()
                        val ktValidBesideUnresolvedArg = unresolvedSiblingAnnos[1].arguments.single()

                        val javaConsumer = resolver.getClassDeclarationByName("JavaDisambiguationConsumer")!!
                        val javaAnnos = javaConsumer.annotations.toList()

                        val javaOuterArrayArg = javaAnnos.single {
                            it.shortName.asString() == "JavaOuterArray"
                        }.arguments.single()
                        val javaNestedList = javaOuterArrayArg.value as List<*>
                        val javaSecondInnerAnno = javaNestedList[1] as KSAnnotation
                        assertTrue(
                            javaSecondInnerAnno.parent is KSValueArgument,
                            "Expected nested Java annotation parent to be KSValueArgument, got ${javaSecondInnerAnno.parent}"
                        )
                        val javaArraySecondArg = javaSecondInnerAnno.arguments.single()

                        val javaOuterDefaultArg = javaAnnos.single {
                            it.shortName.asString() == "JavaOuterDefault"
                        }.arguments.single()
                        val javaDefaultInnerAnno = javaOuterDefaultArg.value as KSAnnotation
                        assertTrue(
                            javaDefaultInnerAnno.parent is KSValueArgument,
                            "Expected default nested Java annotation parent to be KSValueArgument, got ${javaDefaultInnerAnno.parent}"
                        )
                        assertEquals(Origin.SYNTHETIC, javaDefaultInnerAnno.origin)
                        val javaDefaultNestedArg = javaDefaultInnerAnno.arguments.single()
                        assertEquals(
                            Origin.JAVA,
                            javaDefaultNestedArg.origin,
                            "Expected explicit argument inside default nested Java source annotation to have Origin.JAVA"
                        )

                        val javaGeneratedPositionalArg = javaAnnos.single {
                            it.shortName.asString() == "GeneratedJavaRef"
                        }.arguments.single()

                        val rawArgs = linkedMapOf(
                            "ktArraySecond" to ktArraySecondArg,
                            "ktRepeatableSecond" to ktRepeatableSecondArg,
                            "ktDupSecond" to ktDupSecondArg,
                            "ktValidBesideUnresolved" to ktValidBesideUnresolvedArg,
                            "javaArraySecond" to javaArraySecondArg,
                            "javaDefaultNested" to javaDefaultNestedArg,
                            "javaGeneratedPositional" to javaGeneratedPositionalArg,
                        )
                        for ((k, v) in rawArgs) {
                            deferredArgs[k] = (v as? Deferrable)?.defer()
                        }
                        assertEquals("GeneratedKtRef", unresolvedSiblingAnnos[0].shortName.asString())
                        assertTrue(unresolvedSiblingAnnos[0].annotationType.resolve().isError)
                        unresolvedSiblingAnnos[0].arguments
                        deferredUnresolvedKtAnno = (unresolvedSiblingAnnos[0] as? DeferrableAnnotation)?.deferAnnotation()

                        val unresolvedQualifiedProp = ktHolder.declarations.single {
                            it.simpleName.asString() == "unresolvedQualifiedProp"
                        } as KSPropertyDeclaration
                        val unresolvedQualifiedAnno = unresolvedQualifiedProp.annotations.single()
                        assertTrue(unresolvedQualifiedAnno.annotationType.resolve().isError)
                        deferredNestedAnnos["unresolvedQualified"] =
                            (unresolvedQualifiedAnno as? DeferrableAnnotation)?.deferAnnotation()

                        val importAliasedProp = ktHolder.declarations.single {
                            it.simpleName.asString() == "importAliasedProp"
                        } as KSPropertyDeclaration
                        val importAliasedAnno = importAliasedProp.annotations.single()
                        deferredNestedAnnos["importAliased"] =
                            (importAliasedAnno as? DeferrableAnnotation)?.deferAnnotation()

                        val typeAliasedProp = ktHolder.declarations.single {
                            it.simpleName.asString() == "typeAliasedProp"
                        } as KSPropertyDeclaration
                        val typeAliasedAnno = typeAliasedProp.annotations.single()
                        deferredNestedAnnos["typeAliased"] =
                            (typeAliasedAnno as? DeferrableAnnotation)?.deferAnnotation()

                        val nullClassIdHolder = resolver.getClassDeclarationByName("GenericUnresolvedAnnoHolder")!!
                        val nullClassIdProp = nullClassIdHolder.declarations
                            .filterIsInstance<KSPropertyDeclaration>()
                            .single { it.simpleName.asString() == "nullClassIdProp" }
                        assertTrue(nullClassIdProp.annotations.single().annotationType.resolve().isError)

                        val javaPkgConsumer = resolver.getClassDeclarationByName("pkg.JavaPkgNestedConsumer")!!
                        val classOuterAnno = javaPkgConsumer.annotations.single()
                        val psiNestedSingle = classOuterAnno.arguments
                            .single { it.name?.asString() == "value" }.value as KSAnnotation
                        val psiNestedArrayItem = (
                            classOuterAnno.arguments
                                .single { it.name?.asString() == "items" }.value as List<*>
                            ).single() as KSAnnotation
                        assertEquals("GeneratedJavaNested", psiNestedSingle.shortName.asString())
                        assertEquals("GeneratedJavaNested", psiNestedArrayItem.shortName.asString())
                        deferredNestedAnnos["psiSingle"] = (psiNestedSingle as? DeferrableAnnotation)?.deferAnnotation()
                        deferredNestedAnnos["psiArray"] =
                            (psiNestedArrayItem as? DeferrableAnnotation)?.deferAnnotation()

                        val consumeFn = javaPkgConsumer.declarations.filterIsInstance<KSFunctionDeclaration>()
                            .single { it.simpleName.asString() == "consume" }
                        val paramAnnos = consumeFn.parameters.single().annotations.toList()
                        val paramUnresolvedDirectAnno = paramAnnos[0]
                        assertEquals("GeneratedJavaNested", paramUnresolvedDirectAnno.shortName.asString())
                        assertTrue(paramUnresolvedDirectAnno.annotationType.resolve().isError)
                        deferredNestedAnnos["aaDirectParam"] =
                            (paramUnresolvedDirectAnno as? DeferrableAnnotation)?.deferAnnotation()
                        val paramOuterAnno = paramAnnos[1]
                        val aaSingleArg = paramOuterAnno.arguments.single { it.name?.asString() == "value" }
                        val aaItemsArg = paramOuterAnno.arguments.single { it.name?.asString() == "items" }
                        // FIR converts unresolved nested annotation calls into UnsupportedValue (null) in Round 1.
                        aaSingleArg.value
                        aaItemsArg.value
                        deferredNestedContainerArgs["aaSingle"] = (aaSingleArg as? Deferrable)?.defer()
                        deferredNestedContainerArgs["aaArray"] = (aaItemsArg as? Deferrable)?.defer()

                        // Exercise Java package-info.java annotations (both resolved and unresolved in Round 1).
                        val pkgAnnos = resolver.getPackageAnnotations("pkg").toList()
                        val pkgInfoResolvedAnno = pkgAnnos.single {
                            it.shortName.asString() == "PkgInfoResolvedRef"
                        }
                        val pkgInfoUnresolvedAnno = pkgAnnos.single {
                            it.shortName.asString() == "GeneratedPkgInfoAnno"
                        }
                        deferredArgs["pkgInfoResolvedArg"] =
                            (pkgInfoResolvedAnno.arguments.single() as? Deferrable)?.defer()
                        deferredArgs["pkgInfoUnresolvedArg"] =
                            (pkgInfoUnresolvedAnno.arguments.single() as? Deferrable)?.defer()
                        deferredNestedAnnos["pkgInfoUnresolvedAnno"] =
                            (pkgInfoUnresolvedAnno as? DeferrableAnnotation)?.deferAnnotation()

                        // Also exercise central recordClassReferenceLookup guard on empty / unresolved / <error> FQNs.
                        ResolverAAImpl.instance.incrementalContext.recordClassReferenceLookup("", javaPkgConsumer)
                        ResolverAAImpl.instance.incrementalContext.recordClassReferenceLookup(
                            "__KSP_unresolved_GeneratedJavaNested",
                            javaPkgConsumer
                        )
                        ResolverAAImpl.instance.incrementalContext.recordClassReferenceLookup(
                            "<error>",
                            javaPkgConsumer
                        )

                        round1Lookups = ResolverAAImpl.instance.incrementalContext.dumpLookupRecords()

                        env.codeGenerator.createNewFile(Dependencies(false), "", "GeneratedKtRef", "kt").use {
                            it.write(
                                """
                                annotation class GeneratedKtRef
                                """.trimIndent().toByteArray()
                            )
                        }
                        env.codeGenerator.createNewFile(Dependencies(false), "pkg", "GeneratedPkgKtRef", "kt").use {
                            it.write(
                                """
                                package pkg
                                import kotlin.reflect.KClass
                                annotation class GeneratedPkgKtRef(val value: KClass<*> = pkg.PkgTargetFirst::class)
                                """.trimIndent().toByteArray()
                            )
                        }
                        env.codeGenerator.createNewFile(Dependencies(false), "pkg", "GeneratedJavaNested", "java").use {
                            it.write(
                                """
                                package pkg;
                                public @interface GeneratedJavaNested {
                                    Class<?> value();
                                }
                                """.trimIndent().toByteArray()
                            )
                        }
                        env.codeGenerator.createNewFile(Dependencies(false), "pkg", "GeneratedPkgInfoAnno", "java").use {
                            it.write(
                                """
                                package pkg;
                                import java.lang.annotation.ElementType;
                                import java.lang.annotation.Target;
                                @Target(ElementType.PACKAGE)
                                public @interface GeneratedPkgInfoAnno {
                                    Class<?> value();
                                }
                                """.trimIndent().toByteArray()
                            )
                        }

                        env.codeGenerator.createNewFile(Dependencies(false), "", "GeneratedJavaRef", "java").use {
                            it.write(
                                """
                                public @interface GeneratedJavaRef {
                                    Class<?> value();
                                }
                                """.trimIndent().toByteArray()
                            )
                        }
                        // Round 2 is triggered by file generation above; rawArgs are restored manually via Deferrable.defer().
                        return emptyList()
                    } else if (roundCount == 2) {
                        for ((key, restorable) in deferredArgs) {
                            assertNotNull(restorable, "Expected Deferrable.defer() to be non-null for $key")
                            val restored = restorable!!.restore() as? KSValueArgument
                            assertNotNull(restored, "Expected Restorable.restore() to return KSValueArgument for $key")
                            val typeName = (restored!!.value as KSType).declaration.simpleName.asString()
                            restoredSummaries.add("$key->$typeName")
                        }
                        restoredUnresolvedKtAnnoTypeName =
                            deferredUnresolvedKtAnno?.invoke()?.annotationType?.resolve()?.declaration?.simpleName?.asString()
                        for ((key, restorer) in deferredNestedAnnos) {
                            assertNotNull(restorer, "Expected nested annotation deferAnnotation() to be non-null for $key")
                            val restoredNested = restorer!!.invoke()
                            assertNotNull(restoredNested, "Expected nested annotation to restore in Round 2 for $key")
                            val innerTarget = (restoredNested!!.arguments.single().value as KSType)
                                .declaration.simpleName.asString()
                            restoredNestedAnnotationFqNames.add(
                                "$key->${restoredNested.annotationType.resolve().declaration.qualifiedName?.asString()}($innerTarget)"
                            )
                        }
                        for ((key, restorable) in deferredNestedContainerArgs) {
                            val restoredArg = restorable!!.restore() as KSValueArgument
                            val restoredNested = when (val v = restoredArg.value) {
                                is KSAnnotation -> v
                                is List<*> -> v.single() as KSAnnotation
                                else -> error("Unexpected restored value for $key: $v")
                            }
                            val innerTarget = (restoredNested.arguments.single().value as KSType)
                                .declaration.simpleName.asString()
                            restoredNestedAnnotationFqNames.add(
                                "$key->${restoredNested.annotationType.resolve().declaration.qualifiedName?.asString()}($innerTarget)"
                            )
                        }
                        round2Lookups = ResolverAAImpl.instance.incrementalContext.dumpLookupRecords()
                    }
                    return emptyList()
                }
            }
        }

        val kspConfig = KSPJvmConfig.Builder().apply {
            moduleName = "main"
            sourceRoots = listOf(kotlinSrc)
            javaSourceRoots = listOf(javaSrc)
            jdkHome = File(System.getProperty("java.home"))
            jvmTarget = JvmTarget.DEFAULT.description
            languageVersion = "2.0"
            apiVersion = "2.0"
            libraries = listOf(File("dist/kotlinc/lib/kotlin-stdlib.jar"))
            projectBaseDir = tempDir
            classOutputDir = File(tempDir, "out/classes")
            javaOutputDir = File(tempDir, "out/java")
            kotlinOutputDir = File(tempDir, "out/kotlin")
            resourceOutputDir = File(tempDir, "out/resources")
            cachesDir = File(tempDir, "out/caches")
            outputBaseDir = File(tempDir, "out")
            incremental = true
            this.experimentalPsiResolution = experimentalPsiResolution
        }.build()

        val exitCode = KotlinSymbolProcessing(kspConfig, listOf(provider), CommandLineKSPLogger()).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exitCode)
        assertEquals(2, roundCount)
        assertEquals(
            listOf(
                "ktArraySecond->TargetSecond",
                "ktRepeatableSecond->TargetSecond",
                "ktDupSecond->TargetSecond",
                "ktValidBesideUnresolved->TargetValidBesideUnresolved",
                "javaArraySecond->TargetJavaNestedExplicit",
                "javaDefaultNested->TargetJavaNestedDefault",
                "javaGeneratedPositional->TargetGeneratedJavaPositional",
                "pkgInfoResolvedArg->PkgTargetFirst",
                "pkgInfoUnresolvedArg->PkgTargetSecond",
            ),
            restoredSummaries
        )
        assertEquals("GeneratedKtRef", restoredUnresolvedKtAnnoTypeName)
        assertEquals(
            listOf(
                "unresolvedQualified->pkg.GeneratedPkgKtRef(PkgTargetFirst)",
                "importAliased->pkg.GeneratedPkgKtRef(PkgTargetSecond)",
                "typeAliased->pkg.GeneratedPkgKtRef(PkgTargetSecond)",
                "psiSingle->pkg.GeneratedJavaNested(PkgTargetFirst)",
                "psiArray->pkg.GeneratedJavaNested(PkgTargetSecond)",
                "aaDirectParam->pkg.GeneratedJavaNested(PkgTargetFirst)",
                "pkgInfoUnresolvedAnno->pkg.GeneratedPkgInfoAnno(PkgTargetSecond)",
            ),
            restoredNestedAnnotationFqNames.take(7)
        )
        assertEquals(
            listOf(
                "aaSingle->pkg.GeneratedJavaNested(PkgTargetFirst)",
                "aaArray->pkg.GeneratedJavaNested(PkgTargetSecond)",
            ),
            restoredNestedAnnotationFqNames.drop(7)
        )
        assertTrue(
            "javaSrc/pkg/package-info.java" in (round2Lookups["pkg.PkgTargetFirst"] ?: emptyList()),
            "Expected Round 2 lookup for pkg.PkgTargetFirst from package-info.java, got: ${round2Lookups["pkg.PkgTargetFirst"]}"
        )
        assertTrue(
            "javaSrc/pkg/package-info.java" in (round2Lookups["pkg.PkgTargetSecond"] ?: emptyList()),
            "Expected Round 2 lookup for pkg.PkgTargetSecond from package-info.java, got: ${round2Lookups["pkg.PkgTargetSecond"]}"
        )
        assertTrue(
            "javaSrc/pkg/package-info.java" in (round2Lookups["pkg.GeneratedPkgInfoAnno"] ?: emptyList()),
            "Expected Round 2 lookup for pkg.GeneratedPkgInfoAnno from package-info.java, got: ${round2Lookups["pkg.GeneratedPkgInfoAnno"]}"
        )
        assertFalse(
            "<anonymous>." in round1Lookups,
            "Expected no empty-FQN lookup in Round 1, got: $round1Lookups"
        )
        assertTrue(
            round1Lookups.keys.none { "__KSP_unresolved_" in it || "<error>" in it },
            "Expected no unresolved/error lookups in Round 1, got: ${round1Lookups.keys.filter { "__KSP_unresolved_" in it || "<error>" in it }}"
        )
        assertFalse(
            "<anonymous>.GeneratedJavaNested" in round1Lookups,
            "Expected no bogus root-package GeneratedJavaNested lookup in Round 1, got: $round1Lookups"
        )
        assertFalse(
            "<anonymous>.GeneratedJavaNested" in round2Lookups,
            "Expected no bogus root-package GeneratedJavaNested lookup in Round 2, got: $round2Lookups"
        )
        assertTrue(
            round2Lookups.keys.none { "__KSP_unresolved_" in it || "<error>" in it },
            "Expected no unresolved/error lookups in Round 2, got: ${round2Lookups.keys.filter { "__KSP_unresolved_" in it || "<error>" in it }}"
        )
        assertTrue(
            "javaSrc/pkg/JavaPkgNestedConsumer.java" in (round2Lookups["pkg.GeneratedJavaNested"] ?: emptyList()),
            "Expected Round 2 lookup for pkg.GeneratedJavaNested from JavaPkgNestedConsumer.java, got: ${round2Lookups["pkg.GeneratedJavaNested"]}"
        )
        assertTrue(
            "javaSrc/pkg/JavaPkgNestedConsumer.java" in (round2Lookups["pkg.PkgTargetFirst"] ?: emptyList()),
            "Expected Round 2 lookup for pkg.PkgTargetFirst from JavaPkgNestedConsumer.java, got: ${round2Lookups["pkg.PkgTargetFirst"]}"
        )
        assertTrue(
            "javaSrc/pkg/JavaPkgNestedConsumer.java" in (round2Lookups["pkg.PkgTargetSecond"] ?: emptyList()),
            "Expected Round 2 lookup for pkg.PkgTargetSecond from JavaPkgNestedConsumer.java, got: ${round2Lookups["pkg.PkgTargetSecond"]}"
        )
    }

    @Test
    fun testArrayClassLiteralLookupsAA(@TempDir tempDir: File) {
        runArrayClassLiteralLookupsTest(experimentalPsiResolution = false, tempDir = tempDir)
    }

    @Test
    fun testArrayClassLiteralLookupsPsi(@TempDir tempDir: File) {
        runArrayClassLiteralLookupsTest(experimentalPsiResolution = true, tempDir = tempDir)
    }

    @OptIn(KaImplementationDetail::class, KaPlatformInterface::class)
    private fun runArrayClassLiteralLookupsTest(experimentalPsiResolution: Boolean, tempDir: File) {
        val kotlinSrc = File(tempDir, "kotlinSrc").apply { mkdirs() }
        val javaSrc = File(tempDir, "javaSrc").apply { mkdirs() }

        File(kotlinSrc, "ArrayClassLiterals.kt").writeText(
            """
            import kotlin.reflect.KClass

            class KtArrayElem
            class KtMultiDimElem
            class JavaMultiDimDefaultElem
            class JavaMultiDimExplicitElem

            annotation class KtArrayClassAnno(
                val explicit: KClass<*>,
                val defaulted: KClass<*> = Array<Array<KtMultiDimElem>>::class
            )

            @KtArrayClassAnno(explicit = Array<KtArrayElem>::class)
            @JavaArrayClassAnno
            class KtConsumer
            """.trimIndent()
        )

        File(javaSrc, "JavaArrayClassAnno.java").writeText(
            """
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;

            @Retention(RetentionPolicy.RUNTIME)
            public @interface JavaArrayClassAnno {
                Class<?> value() default JavaMultiDimDefaultElem[][].class;
            }
            """.trimIndent()
        )

        File(javaSrc, "JavaConsumer.java").writeText(
            """
            @JavaArrayClassAnno(JavaMultiDimExplicitElem[][].class)
            public class JavaConsumer {}
            """.trimIndent()
        )

        var javaExplicitLeafSummary: String? = null
        var javaDefaultLeafSummary: String? = null
        var lookups: Map<String, List<String>> = emptyMap()

        fun unwrapTwoDimArrayLeaf(type: KSType?): String {
            if (type == null || type.isError) return "ERROR_OUTER"
            val inner = type.arguments.singleOrNull()?.type?.resolve() ?: return "MISSING_INNER"
            if (inner.isError) return "ERROR_INNER"
            val leaf = inner.arguments.singleOrNull()?.type?.resolve() ?: return "MISSING_LEAF"
            if (leaf.isError) return "ERROR_LEAF"
            return "${type.declaration.simpleName.asString()}<${inner.declaration.simpleName.asString()}<${leaf.declaration.simpleName.asString()}>>"
        }

        val provider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    val ktConsumer = resolver.getClassDeclarationByName("KtConsumer")!!
                    ktConsumer.annotations.forEach { anno ->
                        anno.arguments.forEach { arg ->
                            if (anno.shortName.asString() == "JavaArrayClassAnno") {
                                javaDefaultLeafSummary = unwrapTwoDimArrayLeaf(arg.value as? KSType)
                            } else {
                                arg.value
                            }
                        }
                    }
                    val javaConsumer = resolver.getClassDeclarationByName("JavaConsumer")!!
                    javaConsumer.annotations.single().arguments.single().let { arg ->
                        javaExplicitLeafSummary = unwrapTwoDimArrayLeaf(arg.value as? KSType)
                    }

                    val lifetimeToken = KotlinAlwaysAccessibleLifetimeToken(ResolverAAImpl.ktModule.project)
                    val unresolvedEnumVal = object : KaAnnotationValue.EnumEntryValue {
                        override val callableId: CallableId =
                            CallableId(ClassId.fromString("UnresolvedSyntheticEnum"), Name.identifier("ENTRY"))
                        override val sourcePsi: KtCallElement? = null
                        override val token = lifetimeToken
                    }
                    assertSame(KSErrorType, unresolvedEnumVal.toValue(ktConsumer))
                    val unresolvedNestedAnnoVal = object : KaAnnotationValue.NestedAnnotationValue {
                        override val annotation: KaAnnotation = object : KaAnnotation {
                            override val classId: ClassId = ClassId.fromString("UnresolvedSyntheticNestedAnno")
                            override val psi: KtCallElement? = null
                            override val useSiteTarget: AnnotationUseSiteTarget? = null
                            override val arguments: List<KaNamedAnnotationValue> = emptyList()
                            override val constructorSymbol: KaConstructorSymbol? = null
                            override val token = lifetimeToken
                        }
                        override val sourcePsi: KtCallElement? = null
                        override val token = lifetimeToken
                    }
                    unresolvedNestedAnnoVal.toValue(ktConsumer)

                    lookups = ResolverAAImpl.instance.incrementalContext.dumpLookupRecords()
                    return emptyList()
                }
            }
        }

        val kspConfig = KSPJvmConfig.Builder().apply {
            moduleName = "main"
            sourceRoots = listOf(kotlinSrc)
            javaSourceRoots = listOf(javaSrc)
            jdkHome = File(System.getProperty("java.home"))
            jvmTarget = JvmTarget.DEFAULT.description
            languageVersion = "2.0"
            apiVersion = "2.0"
            libraries = listOf(File("dist/kotlinc/lib/kotlin-stdlib.jar"))
            projectBaseDir = tempDir
            classOutputDir = File(tempDir, "out/classes")
            javaOutputDir = File(tempDir, "out/java")
            kotlinOutputDir = File(tempDir, "out/kotlin")
            resourceOutputDir = File(tempDir, "out/resources")
            cachesDir = File(tempDir, "out/caches")
            outputBaseDir = File(tempDir, "out")
            incremental = true
            this.experimentalPsiResolution = experimentalPsiResolution
        }.build()

        val exitCode = KotlinSymbolProcessing(kspConfig, listOf(provider), CommandLineKSPLogger()).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, exitCode)

        assertEquals("Array<Array<JavaMultiDimDefaultElem>>", javaDefaultLeafSummary)
        assertEquals("Array<Array<JavaMultiDimExplicitElem>>", javaExplicitLeafSummary)

        assertTrue("kotlinSrc/ArrayClassLiterals.kt" in (lookups["<anonymous>.KtArrayElem"] ?: emptyList()))
        assertTrue("kotlinSrc/ArrayClassLiterals.kt" in (lookups["<anonymous>.KtMultiDimElem"] ?: emptyList()))
        assertTrue("kotlinSrc/ArrayClassLiterals.kt" in (lookups["<anonymous>.JavaMultiDimDefaultElem"] ?: emptyList()))
        assertTrue("javaSrc/JavaConsumer.java" in (lookups["<anonymous>.JavaMultiDimExplicitElem"] ?: emptyList()))
        assertFalse(
            "<anonymous>.UnresolvedSyntheticEnum" in lookups,
            "Expected unresolved EnumEntryValue ClassId not to be recorded, got: $lookups"
        )
        assertFalse(
            "<anonymous>.UnresolvedSyntheticNestedAnno" in lookups,
            "Expected unresolved NestedAnnotationValue ClassId not to be recorded, got: $lookups"
        )
    }
}
