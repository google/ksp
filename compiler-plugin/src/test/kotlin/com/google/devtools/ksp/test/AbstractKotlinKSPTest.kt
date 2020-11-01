/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.KotlinSymbolProcessingExtension
import com.google.devtools.ksp.KspOptions
import com.google.devtools.ksp.processing.impl.MessageCollectorBasedKSPLogger
import com.google.devtools.ksp.processor.AbstractTestProcessor
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.CodegenTestCase
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.test.*
import java.io.File

abstract class AbstractKotlinKSPTest : KotlinBaseTest<AbstractKotlinKSPTest.KspTestFile>() {
    companion object {
        const val TEST_PROCESSOR = "// TEST PROCESSOR:"
        val EXPECTED_RESULTS = "// EXPECTED:"
    }
    private val testTmpDir by lazy {
        KotlinTestUtils.tmpDir("test")
    }

    override fun doMultiFileTest(wholeFile: File, files: List<KspTestFile>) {
        // get the main module where KSP tests will be run. If there is no module declared in the test input, we'll
        // create one that will contain all test files
        val mainModule: TestModule = files.findLast {
            it.testModule != null
        }?.testModule ?: TestModule("main", emptyList(), emptyList())

        // group each test file with its module
        val filesByModule = groupFilesByModule(mainModule, files)

        // now compile each sub module, while keeping its output folder in moduleOutputs
        filesByModule.forEach { (module, files) ->
            if (module !== mainModule) {
                compileModule(
                    module = module,
                    testFiles = files,
                    dependencies = module.dependencies.map {
                        it.outDir
                    },
                    testProcessor = null)
            }
        }
        // modules are compiled, now compile the test project with KSP
        val testProcessorName = wholeFile
            .readLines()
            .filter { it.startsWith(TEST_PROCESSOR) }
            .single()
            .substringAfter(TEST_PROCESSOR)
            .trim()
        val testProcessor = Class.forName("com.google.devtools.ksp.processor.$testProcessorName").newInstance() as AbstractTestProcessor

        compileModule(
            module = mainModule,
            testFiles = filesByModule[mainModule]!!,
            dependencies = mainModule.dependencies.map { it.outDir },
            testProcessor = testProcessor
        )

        val result = testProcessor.toResult()
        val expectedResults = wholeFile
            .readLines()
            .dropWhile { !it.startsWith(EXPECTED_RESULTS) }
            .drop(1)
            .takeWhile { !it.startsWith("// END") }
            .map { it.substring(3).trim() }
        TestCase.assertEquals(expectedResults.joinToString("\n"), result.joinToString("\n"))
    }

    private fun compileModule(
        module: TestModule,
        testFiles: List<KspTestFile>,
        dependencies: List<File>,
        testProcessor: AbstractTestProcessor?) {
        val moduleRoot = module.rootDir
        module.writeJavaFiles(testFiles)
        val configuration = createConfiguration(
            ConfigurationKind.NO_KOTLIN_REFLECT,
            TestJdkKind.FULL_JDK_9,
            listOf(KotlinTestUtils.getAnnotationsJar()) + dependencies,
            listOf(module.javaSrcDir),
            emptyList()
        )

        val environment = KotlinCoreEnvironment.createForTests(
            testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val moduleFiles = CodegenTestCase.loadMultiFiles(testFiles, environment.project)
        val outDir = module.outDir.also {
            it.mkdirs()
        }
        if (testProcessor != null) {
            val logger = MessageCollectorBasedKSPLogger(
                PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false)
            )
            val analysisExtension =
                KotlinSymbolProcessingExtension(KspOptions.Builder().apply {
                    javaSourceRoots.add(module.javaSrcDir)
                    classOutputDir = File(moduleRoot,"kspTest/classes/main")
                    javaOutputDir = File(moduleRoot,"kspTest/src/main/java")
                    kotlinOutputDir = File(moduleRoot,"kspTest/src/main/kotlin")
                    resourceOutputDir = File(moduleRoot,"kspTest/src/main/resources")
                }.build(), logger, testProcessor)
            val project = environment.project
            AnalysisHandlerExtension.registerExtension(project, analysisExtension)
            GenerationUtils.compileFiles(moduleFiles.psiFiles, environment, ClassBuilderFactories.TEST)
        } else {
            GenerationUtils.compileFilesTo(moduleFiles.psiFiles, environment, outDir)
        }
    }

    /**
     * Groups each file by module. For files that do not have an associated module, they get added to the [mainModule].
     */
    private fun groupFilesByModule(
        mainModule: TestModule,
        testFiles: List<KspTestFile>
    ) : LinkedHashMap<TestModule, MutableList<KspTestFile>> {
        val result = LinkedHashMap<TestModule, MutableList<KspTestFile>>()
        testFiles.forEach { testFile ->
            result.getOrPut(testFile.testModule ?: mainModule) {
                mutableListOf()
            }.add(testFile)
        }
        return result
    }

    /**
     * Write the java files in the given list into the java source directory of the TestModule.
     */
    private fun TestModule.writeJavaFiles(testFiles : List<KspTestFile>) {
        val targetDir = javaSrcDir
        targetDir.mkdirs()
        testFiles.filter {
            it.name.endsWith(".java")
        }.map { testFile ->
            File(targetDir, testFile.name).also {
                it.parentFile.mkdirs()
            }.also {
                it.writeText(
                    testFile.content, Charsets.UTF_8
                )
            }
        }
    }

    private val TestModule.rootDir:File
        get() = File(testTmpDir, name)

    private val TestModule.javaSrcDir:File
        get() = File(rootDir, "javaSrc")

    private val TestModule.outDir:File
        get() = File(rootDir, "out")

    /**
     * TestFile class for KSP where we can also keep a reference to the [TestModule]
     */
    class KspTestFile(
        name: String,
        content: String,
        directives: Directives,
        var testModule: TestModule?
    ) : KotlinBaseTest.TestFile(name, content, directives)
}
