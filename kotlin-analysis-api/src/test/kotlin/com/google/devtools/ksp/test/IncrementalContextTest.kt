/*
 * Copyright 2026 Google LLC
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
import com.google.devtools.ksp.common.IncrementalContext
import com.google.devtools.ksp.impl.DualLookupTracker
import com.google.devtools.ksp.impl.IncrementalContextAA
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class IncrementalContextTest {
    @TempDir
    lateinit var baseDir: File

    @Test
    fun incrementalLifecycleThroughInterface() {
        val context = createContext(incremental = true)
        assertTrue(context.calcDirtyFiles(emptyList()).isEmpty())
        context.registerGeneratedFiles(emptyList())
        assertTrue(context.dumpLookupRecords().isEmpty())
        context.updateCachesAndOutputs(emptyList(), emptySet(), emptyMap())
        assertTrue(File(baseDir, "caches/caches.uptodate").exists())
    }

    @Test
    fun nonIncrementalLifecycleThroughInterface() {
        val context = createContext(incremental = false)
        assertTrue(context.calcDirtyFiles(emptyList()).isEmpty())
        context.registerGeneratedFiles(emptyList())
        assertTrue(context.dumpLookupRecords().isEmpty())
        context.updateCachesAndOutputs(emptyList(), emptySet(), emptyMap())
        assertFalse(File(baseDir, "caches/caches.uptodate").exists())
        context.closeFiles()
    }

    @Test
    fun exceptionHandlingThroughInterface() {
        val context = createContext(incremental = false)
        assertEquals("result", context.closeFilesOnException { "result" })
        val failure = IllegalStateException("processing failed")
        val thrown = assertThrows(IllegalStateException::class.java) {
            context.closeFilesOnException { throw failure }
        }
        assertSame(failure, thrown)
    }

    private fun createContext(incremental: Boolean): IncrementalContext = IncrementalContextAA(
        isIncremental = incremental,
        lookupTracker = DualLookupTracker(),
        anyChangesWildcard = File("<AnyChanges>"),
        loggingOptions = IncrementalContextLoggingOptions(false, null),
        baseDir = baseDir,
        cachesDir = File(baseDir, "caches").apply { mkdirs() },
        kspOutputDir = File(baseDir, "output").apply { mkdirs() },
        knownModified = emptyList(),
        knownRemoved = emptyList(),
        changedClasses = emptyList(),
    )
}
