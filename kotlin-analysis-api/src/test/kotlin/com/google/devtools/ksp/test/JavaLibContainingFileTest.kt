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
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Origin
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.tools.ToolProvider

class JavaLibContainingFileTest {

    @Test
    fun testCompiledJavaContainingFileDeclarationsAndAssociationAA(@TempDir tempDir: File) {
        runCompiledJavaContainingFileTest(experimentalPsiResolution = false, tempDir = tempDir)
    }

    @Test
    fun testCompiledJavaContainingFileDeclarationsAndAssociationPsi(@TempDir tempDir: File) {
        runCompiledJavaContainingFileTest(experimentalPsiResolution = true, tempDir = tempDir)
    }

    private fun runCompiledJavaContainingFileTest(
        experimentalPsiResolution: Boolean,
        tempDir: File,
    ) {
        val javaLibSrc = File(tempDir, "javaLibSrc").apply { mkdirs() }
        val javaLibClasses = File(tempDir, "javaLibClasses").apply { mkdirs() }
        val rootJavaFile = File(javaLibSrc, "RootJavaLib.java").apply {
            writeText("public class RootJavaLib {}\n")
        }
        val javaLibMarkerFile = File(javaLibSrc, "pkg/JavaLibMarker.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package pkg;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                @Retention(RetentionPolicy.RUNTIME)
                public @interface JavaLibMarker {}
                """.trimIndent() + "\n"
            )
        }
        val javaLibParamMarkerFile = File(javaLibSrc, "pkg/JavaLibParamMarker.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package pkg;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                @Retention(RetentionPolicy.RUNTIME)
                public @interface JavaLibParamMarker {}
                """.trimIndent() + "\n"
            )
        }
        val packagedJavaFile = File(javaLibSrc, "pkg/PackagedJavaLib.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package pkg;
                public class PackagedJavaLib {
                    public static class Nested {}
                }
                """.trimIndent() + "\n"
            )
        }
        val annotatedJavaFile = File(javaLibSrc, "pkg/AnnotatedJavaLib.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package pkg;
                @JavaLibMarker
                public class AnnotatedJavaLib {
                    public void libMethod(@JavaLibParamMarker String p) {}
                }
                """.trimIndent() + "\n"
            )
        }

        val onlyNestedJavaFile = File(javaLibSrc, "pkg/OnlyNestedOuter.java").apply {
            parentFile.mkdirs()
            writeText(
                """
                package pkg;
                public class OnlyNestedOuter {
                    public static class OnlyNested {}
                }
                """.trimIndent() + "\n"
            )
        }

        val javac = ToolProvider.getSystemJavaCompiler()
        val compileResult = javac.run(
            null,
            null,
            null,
            "-d",
            javaLibClasses.absolutePath,
            rootJavaFile.absolutePath,
            javaLibMarkerFile.absolutePath,
            javaLibParamMarkerFile.absolutePath,
            packagedJavaFile.absolutePath,
            annotatedJavaFile.absolutePath,
            onlyNestedJavaFile.absolutePath,
        )
        assertEquals(0, compileResult, "Expected javac compilation of test Java library classes to succeed")

        val kotlinSrc = File(tempDir, "kotlinSrc").apply { mkdirs() }
        File(kotlinSrc, "Consumer.kt").writeText("class Consumer(val x: pkg.OnlyNestedOuter.OnlyNested)\n")

        var rootDecls: List<String>? = null
        var packagedDecls: List<String>? = null
        var nestedFileName: String? = null
        var nestedDecls: List<String>? = null
        var nestedMemberDecls: List<String>? = null
        var directNestedFileName: String? = null
        var directNestedDecls: List<String>? = null
        var directNestedMemberDecls: List<String>? = null
        var lookups: Map<String, List<String>> = emptyMap()

        val provider = SymbolProcessorProvider { environment: SymbolProcessorEnvironment ->
            object : SymbolProcessor {
                private var invoked = false

                override fun process(resolver: Resolver): List<KSAnnotated> {
                    if (invoked) return emptyList()
                    invoked = true

                    val rootLib = resolver.getClassDeclarationByName("RootJavaLib")
                    val packagedLib = resolver.getClassDeclarationByName("pkg.PackagedJavaLib")
                    val nestedLib = resolver.getClassDeclarationByName("pkg.PackagedJavaLib.Nested")
                    val consumer = resolver.getClassDeclarationByName("Consumer")
                    assertNotNull(rootLib)
                    assertNotNull(packagedLib)
                    assertNotNull(nestedLib)
                    assertNotNull(consumer)
                    val directNestedLib = consumer!!.primaryConstructor!!.parameters.single().type.resolve().declaration

                    assertEquals(Origin.JAVA_LIB, rootLib!!.origin)
                    assertEquals(Origin.JAVA_LIB, packagedLib!!.origin)
                    assertEquals(Origin.JAVA_LIB, nestedLib!!.origin)
                    assertEquals(Origin.JAVA_LIB, directNestedLib.origin)

                    val rootFile = rootLib.containingFile
                    val packagedFile = packagedLib.containingFile
                    val nestedFile = nestedLib.containingFile
                    val directNestedFile = directNestedLib.containingFile
                    assertNotNull(rootFile)
                    assertNotNull(packagedFile)
                    assertNotNull(nestedFile)
                    assertNotNull(directNestedFile)
                    assertSame(packagedFile, nestedFile)

                    assertEquals(Origin.JAVA_LIB, rootFile!!.origin)
                    assertEquals(Origin.JAVA_LIB, packagedFile!!.origin)
                    assertEquals(Origin.JAVA_LIB, nestedFile!!.origin)
                    assertEquals(Origin.JAVA_LIB, directNestedFile!!.origin)

                    nestedFileName = nestedFile.fileName
                    directNestedFileName = directNestedFile.fileName
                    rootDecls = rootFile.declarations.mapNotNull { it.qualifiedName?.asString() }.toList()
                    packagedDecls = packagedFile.declarations.mapNotNull { it.qualifiedName?.asString() }.toList()
                    nestedDecls = nestedFile.declarations.mapNotNull { it.qualifiedName?.asString() }.toList()
                    nestedMemberDecls = nestedFile.declarations
                        .filterIsInstance<KSClassDeclaration>()
                        .flatMap { it.declarations }
                        .mapNotNull { it.qualifiedName?.asString() }
                        .toList()
                    directNestedDecls = directNestedFile.declarations.mapNotNull { it.qualifiedName?.asString() }.toList()
                    directNestedMemberDecls = directNestedFile.declarations
                        .filterIsInstance<KSClassDeclaration>()
                        .flatMap { it.declarations }
                        .mapNotNull { it.qualifiedName?.asString() }
                        .toList()

                    environment.codeGenerator.createNewFile(
                        Dependencies(false, rootFile, packagedFile, nestedFile, directNestedFile),
                        "",
                        "GeneratedFromJavaLib",
                    ).writer().use {
                        it.write("class GeneratedFromJavaLib\n")
                    }

                    // Resolve library annotationType WITHOUT touching .arguments or .defaultArguments:
                    val annotatedLib = resolver.getClassDeclarationByName("pkg.AnnotatedJavaLib")!!
                    // 1) KSAnnotationJavaImpl (Origin.JAVA_LIB) on AnnotatedJavaLib
                    assertEquals(
                        "pkg.JavaLibMarker",
                        annotatedLib.annotations.single().annotationType.resolve().declaration.qualifiedName?.asString()
                    )
                    // 2) KSAnnotationResolvedImpl (Origin.JAVA_LIB) on AnnotatedJavaLib.libMethod parameter
                    val libMethod = annotatedLib.declarations.filterIsInstance<KSFunctionDeclaration>()
                        .single { it.simpleName.asString() == "libMethod" }
                    assertEquals(
                        "pkg.JavaLibParamMarker",
                        libMethod.parameters.single().annotations.single()
                            .annotationType.resolve().declaration.qualifiedName?.asString()
                    )
                    // 3) KSAnnotationResolvedImpl (Origin.KOTLIN_LIB) on kotlin.PublishedApi
                    val publishedApi = resolver.getClassDeclarationByName("kotlin.PublishedApi")!!
                    assertEquals(Origin.KOTLIN_LIB, publishedApi.origin)
                    val mustBeDocumented = publishedApi.annotations
                        .first { it.shortName.asString() == "MustBeDocumented" }
                    assertEquals(
                        "kotlin.annotation.MustBeDocumented",
                        mustBeDocumented.annotationType.resolve().declaration.qualifiedName?.asString()
                    )

                    lookups = ResolverAAImpl.instance.incrementalContext.dumpLookupRecords()

                    return emptyList()
                }
            }
        }

        val kspConfig = KSPJvmConfig.Builder().apply {
            moduleName = "main"
            sourceRoots = listOf(kotlinSrc)
            javaSourceRoots = emptyList()
            jdkHome = File(System.getProperty("java.home"))
            jvmTarget = JvmTarget.DEFAULT.description
            languageVersion = "2.0"
            apiVersion = "2.0"
            libraries = listOf(File("dist/kotlinc/lib/kotlin-stdlib.jar"), javaLibClasses)
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
        assertEquals(listOf("RootJavaLib"), rootDecls)
        assertEquals(listOf("pkg.PackagedJavaLib"), packagedDecls)
        assertEquals("PackagedJavaLib.class", nestedFileName)
        assertEquals(listOf("pkg.PackagedJavaLib"), nestedDecls)
        assertEquals(listOf("pkg.PackagedJavaLib.Nested"), nestedMemberDecls)
        assertEquals("OnlyNestedOuter.class", directNestedFileName)
        assertEquals(listOf("pkg.OnlyNestedOuter"), directNestedDecls)
        assertEquals(listOf("pkg.OnlyNestedOuter.OnlyNested"), directNestedMemberDecls)
        assertTrue(
            (lookups["pkg.JavaLibMarker"] ?: emptyList()).any { "AnnotatedJavaLib" in it },
            "Expected NoSourceFile lookup for pkg.JavaLibMarker from AnnotatedJavaLib, got: ${lookups["pkg.JavaLibMarker"]}"
        )
        assertTrue(
            (lookups["pkg.JavaLibParamMarker"] ?: emptyList()).any { "AnnotatedJavaLib" in it },
            "Expected NoSourceFile lookup for pkg.JavaLibParamMarker from AnnotatedJavaLib, got: ${lookups["pkg.JavaLibParamMarker"]}"
        )
        assertTrue(
            (lookups["kotlin.annotation.MustBeDocumented"] ?: emptyList()).any { "kotlin.PublishedApi" in it },
            "Expected NoSourceFile lookup for MustBeDocumented from kotlin.PublishedApi, got: ${lookups["kotlin.annotation.MustBeDocumented"]}"
        )
    }
}
