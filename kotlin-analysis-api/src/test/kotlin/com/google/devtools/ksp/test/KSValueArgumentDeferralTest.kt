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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSType
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KSValueArgumentDeferralTest {

    @Test
    fun testArrayClassLiteralLookupsAA(@TempDir tempDir: File) {
        runArrayClassLiteralLookupsTest(experimentalPsiResolution = false, tempDir = tempDir)
    }

    @Test
    fun testArrayClassLiteralLookupsPsi(@TempDir tempDir: File) {
        runArrayClassLiteralLookupsTest(experimentalPsiResolution = true, tempDir = tempDir)
    }

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
    }
}
