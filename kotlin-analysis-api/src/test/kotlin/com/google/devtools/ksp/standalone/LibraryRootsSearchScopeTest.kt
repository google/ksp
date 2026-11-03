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

import com.google.devtools.ksp.test.annotations.Bug
import com.google.devtools.ksp.test.annotations.BugState
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.invariantSeparatorsPathString

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

    private fun createJar(name: String): Path {
        val jar = tempDir.resolve(name)
        ZipOutputStream(Files.newOutputStream(jar)).use { output ->
            output.putNextEntry(ZipEntry("example/Example.class"))
            output.write(byteArrayOf(0))
            output.closeEntry()
        }
        return jar
    }
}
