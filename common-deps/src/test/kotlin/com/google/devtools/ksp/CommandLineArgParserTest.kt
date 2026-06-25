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

package com.google.devtools.ksp

import com.google.devtools.ksp.processing.kspJvmArgParser
import com.google.devtools.ksp.processing.kspJvmArgParserHelp
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class CommandLineArgParserTest(private val isExperimentalPsiResolution: Boolean) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "experimentalPsiResolution={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }

    val sep = File.pathSeparator
    fun mkDefaultArgs() = mutableListOf<String>(
        "-module-name=MyModule",
        "-source-roots", "/path/to/A$sep/path/to/B",
        "/path/to/processorA.jar",
        "-kotlin-output-dir=/path/to/output/kotlin",
        "-java-output-dir=/path/to/output/java",
        "-class-output-dir=/path/to/output/class",
        "-resource-output-dir=/path/to/output/resource",
        "-language-version=2.0",
        "-api-version=2.0",
        "-jvm-target", "21",
        "-project-base-dir", "/path/to/base",
        "-output-base-dir", "/path/to/output",
        "-caches-dir", "/path/to/caches",
        "-experimental-psi-resolution=$isExperimentalPsiResolution",
        "/path/to/processorB.jar${sep}rel/to/processorC.jar",
    )

    @Test
    fun testJvm() {
        val args = mkDefaultArgs().toTypedArray()
        val (config, classpath) = kspJvmArgParser(args)
        Assert.assertEquals(
            listOf("/path/to/A", "/path/to/B").map(::File),
            config.sourceRoots
        )
        Assert.assertEquals(
            "MyModule",
            config.moduleName
        )
        Assert.assertEquals(
            isExperimentalPsiResolution,
            config.experimentalPsiResolution
        )
        Assert.assertEquals(
            listOf("/path/to/processorA.jar", "/path/to/processorB.jar", "rel/to/processorC.jar"),
            classpath
        )
    }

    @Test
    fun testJvmHelp() {
        val helpMsg = kspJvmArgParserHelp()
        Assert.assertTrue("*   -java-output-dir=File" in helpMsg)
        Assert.assertTrue("    -libraries=List<File>" in helpMsg)
        Assert.assertTrue("    -experimental-psi-resolution=Boolean" in helpMsg)
        Assert.assertTrue("*   <processor classpath>" in helpMsg)
        println(helpMsg)
    }

    fun mkDependencyGraphArg(value: String) = "-incremental-log-graph-origin=$value"

    @Test
    fun testIncrementalLoggingGraphOriginNull() {
        val args = mkDefaultArgs()
        val (config, _) = kspJvmArgParser(args.toTypedArray())
        Assert.assertEquals(null, config.incrementalContextLoggingOptions.dependencyGraphOriginName)
    }

    @Test
    fun testIncrementalLoggingGraphOriginValidName() {
        val args = mkDefaultArgs()
        args.add(
            mkDependencyGraphArg("a.b.c.MyClass")
        )
        val (config, _) = kspJvmArgParser(args.toTypedArray())
        Assert.assertEquals("a.b.c.MyClass", config.incrementalContextLoggingOptions.dependencyGraphOriginName)
    }

    @Test
    fun testIncrementalLoggingGraphOriginEmptyQuotes() {
        val args = mkDefaultArgs()
        args.add(
            mkDependencyGraphArg("\"\"")
        )
        val (config, _) = kspJvmArgParser(args.toTypedArray())
        Assert.assertEquals("", config.incrementalContextLoggingOptions.dependencyGraphOriginName)
    }

    @Test
    fun testIncrementalLoggingGraphOriginValidNameWithQuotes() {
        val args = mkDefaultArgs()
        args.add(
            mkDependencyGraphArg("\"xyz.Something\"")
        )
        val (config, _) = kspJvmArgParser(args.toTypedArray())
        Assert.assertEquals("xyz.Something", config.incrementalContextLoggingOptions.dependencyGraphOriginName)
    }

    @Test
    fun testIncrementalLoggingGraphOriginInvalidChars() {
        val args = mkDefaultArgs()
        args.add(
            mkDependencyGraphArg("\\\"xyz.Other")
        )
        val (config, _) = kspJvmArgParser(args.toTypedArray())
        Assert.assertEquals("xyz.Other", config.incrementalContextLoggingOptions.dependencyGraphOriginName)
    }
}
