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

package com.google.devtools.ksp.standalone

import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processing.KSPJvmConfig
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.test.annotations.Bug
import com.google.devtools.ksp.test.annotations.BugState
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.invariantSeparatorsPathString

@Isolated
@Execution(ExecutionMode.SAME_THREAD)
class LibraryRootsSearchScopeTest {
    @TempDir
    lateinit var tempDir: Path

    @Bug("https://github.com/google/ksp/issues/3286", BugState.FIXED)
    @Test
    fun containsLibraryClassesAfterJarHandlersAreCleared() {
        val library = createJar("library.jar").invariantSeparatorsPathString
        val otherLibrary = createJar("library.jar.other.jar").invariantSeparatorsPathString
        val fileSystem = CoreJarFileSystem()
        try {
            val originalRoot =
                requireNotNull(fileSystem.findFileByPath("$library!/"))
            val originalClass =
                requireNotNull(
                    fileSystem.findFileByPath("$library!/example/Example.class")
                )
            val otherClass =
                requireNotNull(
                    fileSystem.findFileByPath("$otherLibrary!/example/Example.class")
                )
            val scope = LibraryRootsSearchScope(listOf(originalRoot))
            assertTrue(scope.contains(originalClass))
            assertFalse(scope.contains(otherClass))

            fileSystem.clearHandlersCache()

            val recreatedRoot =
                requireNotNull(fileSystem.findFileByPath("$library!/"))
            val recreatedClass =
                requireNotNull(
                    fileSystem.findFileByPath("$library!/example/Example.class")
                )

            assertNotSame(originalRoot, recreatedRoot)
            assertFalse(scope.contains(otherClass))
            assertTrue(scope.contains(recreatedClass))
            assertTrue(scope.contains(originalClass))
        } finally {
            // Release cached ZipFile handles so JUnit can delete @TempDir on Windows.
            fileSystem.clearHandlersCache()
        }
    }

    @Test
    fun doesNotClearSharedJarHandlersDuringConcurrentExecutions() {
        val library = createJar("library.jar")
        val libraryPath = library.invariantSeparatorsPathString
        val bothStarted = CountDownLatch(2)
        val firstCanFinish = CountDownLatch(1)
        val firstFinished = CountDownLatch(1)

        val firstProvider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    assertNotNull(
                        resolver.getClassDeclarationByName(
                            resolver.getKSNameFromString("example.Example")
                        )
                    )
                    bothStarted.countDown()
                    assertTrue(firstCanFinish.await(30, TimeUnit.SECONDS))
                    return emptyList()
                }
            }
        }

        val secondProvider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    bothStarted.countDown()
                    assertTrue(bothStarted.await(30, TimeUnit.SECONDS))
                    val jarFs = StandardFileSystems.jar()
                    val rootWhileBothActive = requireNotNull(jarFs.findFileByPath("$libraryPath!/"))

                    firstCanFinish.countDown()
                    assertTrue(firstFinished.await(30, TimeUnit.SECONDS))

                    val rootAfterFirstFinished = requireNotNull(jarFs.findFileByPath("$libraryPath!/"))
                    assertSame(rootWhileBothActive, rootAfterFirstFinished)
                    assertNotNull(
                        resolver.getClassDeclarationByName(
                            resolver.getKSNameFromString("example.Example")
                        )
                    )
                    return emptyList()
                }
            }
        }

        val executor = Executors.newFixedThreadPool(2)
        try {
            val firstFuture = executor.submit<KotlinSymbolProcessing.ExitCode> {
                try {
                    KotlinSymbolProcessing(
                        createKspConfig("module1", library),
                        listOf(firstProvider),
                        CommandLineKSPLogger()
                    ).execute()
                } finally {
                    firstFinished.countDown()
                }
            }
            val secondFuture = executor.submit<KotlinSymbolProcessing.ExitCode> {
                KotlinSymbolProcessing(
                    createKspConfig("module2", library),
                    listOf(secondProvider),
                    CommandLineKSPLogger()
                ).execute()
            }

            assertEquals(KotlinSymbolProcessing.ExitCode.OK, firstFuture.get(60, TimeUnit.SECONDS))
            assertEquals(KotlinSymbolProcessing.ExitCode.OK, secondFuture.get(60, TimeUnit.SECONDS))
        } finally {
            executor.shutdownNow()
        }

        // Verify that after both concurrent executions finish, cached JAR handlers and ZipFile
        // handles are cleared so the JAR can be overwritten and a subsequent execution sees the
        // updated contents.
        createJar("library.jar", className = "example/UpdatedExample")
        var thirdRan = false
        val thirdProvider = SymbolProcessorProvider {
            object : SymbolProcessor {
                override fun process(resolver: Resolver): List<KSAnnotated> {
                    thirdRan = true
                    assertNull(
                        resolver.getClassDeclarationByName(
                            resolver.getKSNameFromString("example.Example")
                        )
                    )
                    assertNotNull(
                        resolver.getClassDeclarationByName(
                            resolver.getKSNameFromString("example.UpdatedExample")
                        )
                    )
                    return emptyList()
                }
            }
        }
        val thirdExitCode = KotlinSymbolProcessing(
            createKspConfig("module3", library),
            listOf(thirdProvider),
            CommandLineKSPLogger()
        ).execute()
        assertEquals(KotlinSymbolProcessing.ExitCode.OK, thirdExitCode)
        assertTrue(thirdRan)
    }

    private fun createKspConfig(moduleName: String, library: Path): KSPJvmConfig {
        val root = Files.createDirectories(tempDir.resolve(moduleName))
        val srcDir = Files.createDirectories(root.resolve("src")).toFile()
        File(srcDir, "Source.kt").writeText("package test\nclass Source\n")
        return KSPJvmConfig.Builder().apply {
            this.moduleName = moduleName
            sourceRoots = listOf(srcDir)
            libraries = listOf(library.toFile())
            projectBaseDir = root.toFile()
            outputBaseDir = root.resolve("out").toFile()
            cachesDir = root.resolve("caches").toFile()
            classOutputDir = root.resolve("out/classes").toFile()
            kotlinOutputDir = root.resolve("out/kotlin").toFile()
            javaOutputDir = root.resolve("out/java").toFile()
            resourceOutputDir = root.resolve("out/res").toFile()
            jvmTarget = "1.8"
            languageVersion = "2.0"
            apiVersion = "2.0"
        }.build()
    }

    private fun createJar(name: String, className: String = "example/Example"): Path {
        val jar = tempDir.resolve(name)
        val cw = ClassWriter(0)
        cw.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER,
            className,
            null,
            "java/lang/Object",
            null
        )
        cw.visitEnd()
        ZipOutputStream(Files.newOutputStream(jar)).use { output ->
            output.putNextEntry(ZipEntry("$className.class"))
            output.write(cw.toByteArray())
            output.closeEntry()
        }
        return jar
    }
}
