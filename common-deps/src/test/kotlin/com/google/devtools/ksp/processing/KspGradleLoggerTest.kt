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

package com.google.devtools.ksp.processing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class KspGradleLoggerTest {

    @Test
    fun testWarnWithFix() {
        val out = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(out))
        try {
            val logger = KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_WARN)
            val fix = KSPSuggestedFix("NewAnnotation", "Replace with NewAnnotation")
            logger.warn("Deprecated annotation", null, fix)

            val output = out.toString().trim()
            assertEquals(
                "w: [ksp] Deprecated annotation -> Suggested Fix (Replace with NewAnnotation): [NewAnnotation]",
                output
            )
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun testErrorWithFix() {
        val out = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(out))
        try {
            val logger = KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_ERROR)
            val fix = KSPSuggestedFix("replacement")
            logger.error("Something wrong", null, fix)

            val output = out.toString().trim()
            assertEquals(
                "e: [ksp] Something wrong -> Suggested Fix: [replacement]",
                output
            )
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun testWarnWithoutFix() {
        val out = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(out))
        try {
            val logger = KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_WARN)
            logger.warn("Simple warning", null)

            val output = out.toString().trim()
            assertEquals("w: [ksp] Simple warning", output)
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun testErrorWithoutFix() {
        val out = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(out))
        try {
            val logger = KspGradleLogger(KspGradleLogger.LOGGING_LEVEL_ERROR)
            logger.error("Simple error", null)

            val output = out.toString().trim()
            assertEquals("e: [ksp] Simple error", output)
        } finally {
            System.setOut(originalOut)
        }
    }
}
