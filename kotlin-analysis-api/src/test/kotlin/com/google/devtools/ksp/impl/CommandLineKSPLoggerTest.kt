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

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.processing.KSPSuggestedFix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@Execution(ExecutionMode.SAME_THREAD)
class CommandLineKSPLoggerTest {

    @Test
    fun testWarnWithFix() {
        val err = ByteArrayOutputStream()
        val originalErr = System.err
        System.setErr(PrintStream(err))
        try {
            val logger = CommandLineKSPLogger()
            val fix = KSPSuggestedFix("NewAnnotation", "Replace with NewAnnotation")
            logger.warn("Deprecated annotation", null, fix)

            val output = err.toString().trim()
            assertEquals(
                "Warning: Deprecated annotation -> Suggested Fix (Replace with NewAnnotation): [NewAnnotation]",
                output
            )
        } finally {
            System.setErr(originalErr)
        }
    }

    @Test
    fun testErrorWithFix() {
        val err = ByteArrayOutputStream()
        val originalErr = System.err
        System.setErr(PrintStream(err))
        try {
            val logger = CommandLineKSPLogger()
            val fix = KSPSuggestedFix("replacement")
            logger.error("Something wrong", null, fix)

            val output = err.toString().trim()
            assertEquals(
                "Error: Something wrong -> Suggested Fix: [replacement]",
                output
            )
        } finally {
            System.setErr(originalErr)
        }
    }

    @Test
    fun testWarnWithoutFix() {
        val err = ByteArrayOutputStream()
        val originalErr = System.err
        System.setErr(PrintStream(err))
        try {
            val logger = CommandLineKSPLogger()
            logger.warn("Simple warning", null)

            val output = err.toString().trim()
            assertEquals("Simple warning", output)
        } finally {
            System.setErr(originalErr)
        }
    }

    @Test
    fun testErrorWithoutFix() {
        val err = ByteArrayOutputStream()
        val originalErr = System.err
        System.setErr(PrintStream(err))
        try {
            val logger = CommandLineKSPLogger()
            logger.error("Simple error", null)

            val output = err.toString().trim()
            assertEquals("Simple error", output)
        } finally {
            System.setErr(originalErr)
        }
    }
}
