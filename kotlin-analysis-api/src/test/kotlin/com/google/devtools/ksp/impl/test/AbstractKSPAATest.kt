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

import com.google.devtools.ksp.KspOptions
import com.google.devtools.ksp.impl.CommandLineKSPLogger
import com.google.devtools.ksp.impl.KotlinSymbolProcessing
import com.google.devtools.ksp.processor.AbstractTestProcessor
import com.google.devtools.ksp.testutils.AbstractKSPTest
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.kotlinPathsForDistDirectoryForTests
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.isKtFile
import org.jetbrains.kotlin.test.services.javaFiles
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files

abstract class AbstractKSPAATest : AbstractKSPTest(FrontendKinds.FIR) {
    val TestModule.kotlinSrc
        get() = File(testRoot, "kotlinSrc")

    fun TestModule.writeKtFiles() {
        kotlinSrc.mkdirs()
        files.filter { it.isKtFile }.forEach { file ->
            File(kotlinSrc, file.relativePath).let {
                it.parentFile.mkdirs()
                it.writeText(file.originalContent)
            }
        }
    }

    private fun compileKotlin(sourcesPath: String, outDir: File) {
        val classpath = mutableListOf<String>()
        if (File(sourcesPath).isDirectory) {
            classpath += sourcesPath
        }
        classpath += PathUtil.kotlinPathsForDistDirectoryForTests.stdlibPath.path

        val args = mutableListOf(
            sourcesPath,
            "-d", outDir.absolutePath,
            "-no-stdlib",
            "-classpath", classpath.joinToString(File.pathSeparator)
        )
        runJvmCompiler(args)
    }

    private fun runJvmCompiler(args: List<String>) {
        val outStream = ByteArrayOutputStream()
        val compilerClass = URLClassLoader(arrayOf(), javaClass.classLoader).loadClass(K2JVMCompiler::class.java.name)
        val compiler = compilerClass.newInstance()
        val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)
        execMethod.invoke(compiler, PrintStream(outStream), args.toTypedArray())
    }

    override fun compileModule(module: TestModule, testServices: TestServices) {
        module.writeKtFiles()
        val javaFiles = module.writeJavaFiles()
        compileKotlin(module.kotlinSrc.path, module.outDir)
        val dependencies = module.allDependencies.map { outDirForModule(it.moduleName) }
        val classpath = (dependencies + KtTestUtil.getAnnotationsJar() + module.outDir)
            .joinToString(File.pathSeparator) { it.absolutePath }
        val options = listOf(
            "-classpath", classpath,
            "-d", module.outDir.path
        )
        if (javaFiles.isNotEmpty()) {
            compileJavaFiles(javaFiles, options, assertions = JUnit5Assertions)
        }
    }

    override fun runTest(
        testServices: TestServices,
        mainModule: TestModule,
        libModules: List<TestModule>,
        testProcessor: AbstractTestProcessor
    ): List<String> {
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(mainModule)
        compilerConfiguration.put(CommonConfigurationKeys.MODULE_NAME, mainModule.name)
        compilerConfiguration.addKotlinSourceRoot(mainModule.kotlinSrc.absolutePath)
        mainModule.kotlinSrc.mkdirs()
        if (!mainModule.javaFiles.isEmpty()) {
            mainModule.writeJavaFiles()
            compilerConfiguration.addJavaSourceRoot(mainModule.javaDir)
        }

        // Some underlying service needs files backed by local fs.
        // Therefore, this doesn't work:
        //  val ktFiles = mainModule.loadKtFiles(kotlinCoreEnvironment.project)
        mainModule.writeKtFiles()
        val kotlinSourceFiles = mainModule.files.filter { it.isKtFile }.map {
            File(mainModule.kotlinSrc, it.relativePath)
        }
        val ktSourceRoots = kotlinSourceFiles
            .sortedBy { Files.isSymbolicLink(it.toPath()) } // Get non-symbolic paths first
            .distinctBy { it.canonicalPath }
        compilerConfiguration.addKotlinSourceRoots(ktSourceRoots.map { it.absolutePath })

        val testRoot = mainModule.testRoot

        val kspOptions = KspOptions.Builder().apply {
            if (!mainModule.javaFiles.isEmpty()) {
                javaSourceRoots.add(mainModule.javaDir)
            }
            classOutputDir = File(testRoot, "kspTest/classes/main")
            javaOutputDir = File(testRoot, "kspTest/src/main/java")
            kotlinOutputDir = File(testRoot, "kspTest/src/main/kotlin")
            resourceOutputDir = File(testRoot, "kspTest/src/main/resources")
            projectBaseDir = testRoot
            cachesDir = File(testRoot, "kspTest/kspCaches")
            kspOutputDir = File(testRoot, "kspTest")
        }.build()
        val analysisSession = buildStandaloneAnalysisAPISession {
            buildKtModuleProviderByCompilerConfiguration(compilerConfiguration)
        }
        val ksp = KotlinSymbolProcessing(
            compilerConfiguration,
            kspOptions,
            CommandLineKSPLogger(),
            analysisSession,
            listOf(testProcessor)
        )
        ksp.prepare()
        ksp.execute()
        return testProcessor.toResult()
    }
}
