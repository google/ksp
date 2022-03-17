/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.impl.test

import com.google.devtools.ksp.DualLookupTracker
import com.google.devtools.ksp.impl.ResolverAAImpl
import com.google.devtools.ksp.impl.convertFilesToKtFiles
import com.google.devtools.ksp.processor.AbstractTestProcessor
import com.google.devtools.ksp.testutils.AbstractKSPTest
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.analysis.api.standalone.configureApplicationEnvironment
import org.jetbrains.kotlin.analysis.api.standalone.configureProjectEnvironment
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.isKtFile
import org.jetbrains.kotlin.test.services.javaFiles
import org.junit.jupiter.api.Assertions
import java.io.File

abstract class AbstractKSPAATest : AbstractKSPTest(FrontendKinds.FIR) {
    val TestModule.kotlinSrc
        get() = File(testRoot, "kotlinSrc")

    fun TestModule.writeKtFiles() {
        kotlinSrc.mkdirs()
        files.filter { it.isKtFile }.forEach {
            File(kotlinSrc, it.relativePath).writeText(it.originalContent)
        }
    }

    override fun runTest(testServices: TestServices, mainModule: TestModule, libModules: List<TestModule>) {
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(mainModule)
        compilerConfiguration.put(CommonConfigurationKeys.MODULE_NAME, mainModule.name)
        compilerConfiguration.put(CommonConfigurationKeys.LOOKUP_TRACKER, DualLookupTracker())
        compilerConfiguration.addKotlinSourceRoot(mainModule.kotlinSrc.absolutePath)
        mainModule.kotlinSrc.mkdirs()
        if (!mainModule.javaFiles.isEmpty()) {
            mainModule.writeJavaFiles()
            compilerConfiguration.addJavaSourceRoot(mainModule.javaDir)
        }

        val application = ApplicationManager.getApplication() as MockApplication
        configureApplicationEnvironment(application)

        // TODO: other platforms
        val kotlinCoreEnvironment = KotlinCoreEnvironment.createForTests(
            disposable,
            compilerConfiguration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        // Some underlying service needs files backed by local fs.
        // Therefore, this doesn't work:
        //  val ktFiles = mainModule.loadKtFiles(kotlinCoreEnvironment.project)
        mainModule.writeKtFiles()
        val kotlinSourceFiles = mainModule.files.filter { it.isKtFile }.map {
            File(mainModule.kotlinSrc, it.relativePath)
        }
        val ktFiles = convertFilesToKtFiles(kotlinCoreEnvironment.project, kotlinSourceFiles)

        configureProjectEnvironment(
            kotlinCoreEnvironment.project as MockProject,
            compilerConfiguration,
            kotlinCoreEnvironment::createPackagePartProvider,
            kotlinCoreEnvironment.projectEnvironment.environment.jarFileSystem as CoreJarFileSystem
        )

        val contents = mainModule.files.first().originalFile.readLines()
        val testProcessorName = contents
            .filter { it.startsWith(TEST_PROCESSOR) }
            .single()
            .substringAfter(TEST_PROCESSOR)
            .trim()
        val testProcessor: AbstractTestProcessor =
            Class.forName("com.google.devtools.ksp.processor.$testProcessorName")
                .getDeclaredConstructor().newInstance() as AbstractTestProcessor

        val resolver = ResolverAAImpl(ktFiles)
        testProcessor.process(resolver)

        val result = testProcessor.toResult()
        val expectedResults = contents
            .dropWhile { !it.startsWith(EXPECTED_RESULTS) }
            .drop(1)
            .takeWhile { !it.startsWith("// END") }
            .map { it.substring(3).trim() }
        Assertions.assertEquals(expectedResults.joinToString("\n"), result.joinToString("\n"))
    }
}
