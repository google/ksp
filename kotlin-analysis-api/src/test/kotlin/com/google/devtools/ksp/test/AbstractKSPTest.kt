/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.processor.AbstractTestProcessor
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.ExecutionListenerBasedDisposableProvider
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKind
import org.jetbrains.kotlin.test.model.ResultingArtifact
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File

abstract class DisposableTest {
    private var _disposable: Disposable? = null
    protected val disposable: Disposable get() = _disposable!!

    @BeforeEach
    private fun initDisposable(testInfo: TestInfo) {
        _disposable = Disposer.newDisposable("disposable for ${testInfo.displayName}")
    }

    @AfterEach
    private fun disposeDisposable() {
        _disposable?.let { Disposer.dispose(it) }
        _disposable = null
    }
}

abstract class AbstractKSPTest(frontend: FrontendKind<*>) : DisposableTest() {
    companion object {
        val TEST_PROCESSOR = "// TEST PROCESSOR:"
        val EXPECTED_RESULTS = "// EXPECTED:"
    }

    val kspTestRoot = KtTestUtil.tmpDir("com/google/devtools/ksp/test/testgoogle/devtools/ksp/test/test")
    fun rootDirForModule(name: String) = File(kspTestRoot, name)
    fun outDirForModule(name: String) = File(rootDirForModule(name), "out")
    fun javaDirForModule(name: String) = File(rootDirForModule(name), "javaSrc")
    val TestModule.testRoot: File
        get() = rootDirForModule(name)
    val TestModule.outDir: File
        get() = outDirForModule(name)
    val TestModule.javaDir: File
        get() = javaDirForModule(name)

    protected lateinit var testInfo: KotlinTestInfo
        private set

    @BeforeEach
    fun initTestInfo(testInfo: TestInfo) {
        this.testInfo = KotlinTestInfo(
            className = testInfo.testClass.orElseGet(null)?.name ?: "_undefined_",
            methodName = testInfo.testMethod.orElseGet(null)?.name ?: "_testUndefined_",
            tags = testInfo.tags
        )
    }

    open fun configureTest(builder: TestConfigurationBuilder) = Unit

    abstract fun runTest(
        testServices: TestServices,
        mainModule: TestModule,
        libModules: List<TestModule>,
        testProcessor: AbstractTestProcessor,
    ): List<String>

    private val configure: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            this@globalDefaults.frontend = frontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
        )
        assertions = JUnit5Assertions
        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
        useAdditionalService<ApplicationDisposableProvider> { ExecutionListenerBasedDisposableProvider() }
        useAdditionalService<KotlinStandardLibrariesPathProvider> { StandardLibrariesPathProviderForKotlinProject }

        useDirectives(*AbstractKotlinCompilerTest.defaultDirectiveContainers.toTypedArray())
        useDirectives(JvmEnvironmentConfigurationDirectives)

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
            JvmEnvironmentConfigurationDirectives.JVM_TARGET with JvmTarget.DEFAULT
            +ConfigurationDirectives.WITH_STDLIB
            +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
        }

        configureTest(this)

        startingArtifactFactory = { ResultingArtifact.Source() }
        this.testInfo = this@AbstractKSPTest.testInfo
    }

    fun TestModule.loadKtFiles(project: Project): List<KtFile> {
        return files.filter { it.isKtFile }.map {
            KtTestUtil.createFile(it.name, it.originalContent, project)
        }
    }

    fun TestModule.writeJavaFiles(): List<File> {
        javaDir.mkdirs()
        val files = javaFiles.map { it to File(javaDir, it.relativePath) }
        files.forEach { (testFile, file) ->
            file.parentFile.mkdirs()
            file.writeText(testFile.originalContent)
        }
        return files.map { it.second }
    }

    // No, this is far from complete. It only works for our test cases.
    //
    // No, neither CompiledLibraryProvider nor LibraryEnvironmentConfigurator can be used. They rely on
    // dist/kotlinc/lib/*
    //
    // No, sourceFileProvider doesn't group files by module unfortunately. Let's do it by ourselves.
    open fun compileModule(module: TestModule, testServices: TestServices) {
        val javaFiles = module.writeJavaFiles()
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val dependencies = module.allDependencies.map { outDirForModule(it.moduleName) }
        compilerConfiguration.addJvmClasspathRoots(dependencies)
        compilerConfiguration.addJavaSourceRoot(module.javaDir)

        // TODO: other platforms
        val kotlinCoreEnvironment = KotlinCoreEnvironment.createForTests(
            disposable,
            compilerConfiguration,
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
        val ktFiles = module.loadKtFiles(kotlinCoreEnvironment.project)
        GenerationUtils.compileFilesTo(ktFiles, kotlinCoreEnvironment, module.outDir)

        if (module.javaFiles.isEmpty())
            return

        val classpath = (dependencies + KtTestUtil.getAnnotationsJar() + module.outDir)
            .joinToString(File.pathSeparator) { it.absolutePath }
        val options = listOf(
            "-classpath", classpath,
            "-d", module.outDir.path
        )
        compileJavaFiles(javaFiles, options, assertions = JUnit5Assertions)
    }

    fun runTest(@TestDataFile path: String) {
        val testConfiguration = testConfiguration(path, configure)
        Disposer.register(disposable, testConfiguration.rootDisposable)
        val testServices = testConfiguration.testServices
        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            path,
            testConfiguration.directives,
        )
        val dependencyProvider = DependencyProviderImpl(testServices, moduleStructure.modules)
        testServices.registerDependencyProvider(dependencyProvider)
        testServices.register(TestModuleStructure::class, moduleStructure)

        val mainModule = moduleStructure.modules.last()
        val libModules = moduleStructure.modules.dropLast(1)

        for (lib in libModules) {
            compileModule(lib, testServices)
        }
        val compilerConfigurationMain = testServices.compilerConfigurationProvider.getCompilerConfiguration(mainModule)
        compilerConfigurationMain.addJvmClasspathRoots(libModules.map { it.outDir })

        val contents = mainModule.files.first().originalFile.readLines()

        val testProcessorName = contents
            .filter { it.startsWith(TEST_PROCESSOR) }
            .single()
            .substringAfter(TEST_PROCESSOR)
            .trim()
        val testProcessor: AbstractTestProcessor =
            Class.forName("com.google.devtools.ksp.processor.$testProcessorName")
                .getDeclaredConstructor().newInstance() as AbstractTestProcessor

        val expectedResults = contents
            .dropWhile { !it.startsWith(EXPECTED_RESULTS) }
            .drop(1)
            .takeWhile { !it.startsWith("// END") }
            .map { it.substring(3).trim() }

        val results = runTest(testServices, mainModule, libModules, testProcessor)
        Assertions.assertEquals(expectedResults.joinToString("\n"), results.joinToString("\n"))
    }
}
