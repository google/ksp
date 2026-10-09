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

import com.google.devtools.ksp.ApiFeatures
import com.google.devtools.ksp.InternalKSPException
import com.google.devtools.ksp.processing.parseBoolean
import com.google.devtools.ksp.processor.AbstractTestProcessor
import com.google.devtools.ksp.symbol.NonExistLocation
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.analysis.test.framework.services.TargetPlatformDirectives
import org.jetbrains.kotlin.analysis.test.framework.services.TargetPlatformProviderForAnalysisApiTests
import org.jetbrains.kotlin.cli.common.disposeRootInWriteAction
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.cli.jvm.config.addJavaSourceRoot
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.forTestCompile.TestCompilePaths
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.ExecutionListenerBasedDisposableProvider
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.testConfiguration
import org.jetbrains.kotlin.test.compileJavaFiles
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JsEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.NativeEnvironmentConfigurationDirectives
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
import java.awt.EventQueue
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties

abstract class DisposableTest {
    private var _disposable: Disposable? = null
    protected val disposable: Disposable get() = _disposable!!

    @BeforeEach
    fun initDisposable(testInfo: TestInfo) {
        _disposable = Disposer.newDisposable("disposable for ${testInfo.displayName}")
    }

    @AfterEach
    fun disposeDisposable() {
        _disposable?.let { disposeRootInWriteAction(it) }
        _disposable = null
    }
}

abstract class AbstractKSPTest(frontend: FrontendKind<*>, val apiFeatures: ApiFeatures) : DisposableTest() {
    companion object {
        const val COMMENT_TOKEN = "//"
        const val TEST_PROCESSOR = "$COMMENT_TOKEN TEST PROCESSOR:"
        const val PROCESSOR_INPUT = "$COMMENT_TOKEN PROCESSOR INPUT:"
        const val EXPECTED_RESULTS = "$COMMENT_TOKEN EXPECTED:"

        const val EXPECT_LINE = "$COMMENT_TOKEN EXPECT"

        // TODO: Move this into the ApiFeaturesImpl and convert it from screaming snake case to camel case
        val EXPECT_FEATURES: Set<String> =
            enumeratePropertiesOf(ApiFeatures::class)
                .map(::convertCamelCaseToScreamingSnakeCase)
                .toSet()
        const val EXPECTED_RESULTS_END = "$COMMENT_TOKEN END"
        const val MODULE = "$COMMENT_TOKEN MODULE:"
        const val COMPILER_MODULE_NAME = "$COMMENT_TOKEN COMPILER MODULE NAME:"

        private fun enumeratePropertiesOf(clazz: KClass<*>): List<String> =
            clazz
                .memberProperties
                .map { property ->
                    property.name
                }

        private fun convertCamelCaseToScreamingSnakeCase(camelCaseStr: String): String {
            val camelCaseRegex = Regex("(?<=[a-z0-9])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
            return camelCaseStr
                .replace(camelCaseRegex, "_")
                .uppercase()
        }
    }

    init {
        // Set system properties for runtime jars needed for Kotlin.
        // See https://github.com/JetBrains/kotlin/commit/ad4cd812a64fa7c8241424abaf3998aa3b7f6b60
        // Keep in sync with Gradle build configurations `libsForTesting` and `libsForTestingCommon`
        System.setProperty(TestCompilePaths.KOTLIN_FULL_STDLIB_PATH, "dist/kotlinc/lib/kotlin-stdlib.jar")
        System.setProperty(TestCompilePaths.KOTLIN_COMMON_STDLIB_PATH, "dist/common/lib/kotlin-stdlib.jar")
        System.setProperty(TestCompilePaths.KOTLIN_TEST_JAR_PATH, "dist/kotlinc/lib/kotlin-test.jar")
        System.setProperty(TestCompilePaths.KOTLIN_SCRIPT_RUNTIME_PATH, "dist/kotlinc/lib/kotlin-script-runtime.jar")
        System.setProperty(
            TestCompilePaths.KOTLIN_MOCKJDK_ANNOTATIONS_PATH,
            "third-party/mockJDKs/mockJDK/jre/lib/annotations.jar"
        )
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

    @OptIn(TestInfrastructureInternals::class)
    private val configure: TestConfigurationBuilder.() -> Unit = {
        globalDefaults {
            this@globalDefaults.frontend = frontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Source
        }
        useCustomCompilerConfigurationProvider(::CompilerConfigurationProviderImpl)
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator,
        )
        assertions = JUnit5Assertions
        useAdditionalService<TemporaryDirectoryManager>(::TemporaryDirectoryManagerImpl)
        useAdditionalService<ApplicationDisposableProvider> { ExecutionListenerBasedDisposableProvider() }
        useAdditionalService<KotlinStandardLibrariesPathProvider> { StandardLibrariesPathProviderForKotlinProject }
        useAdditionalService<TargetPlatformProvider>(::TargetPlatformProviderForAnalysisApiTests)

        useDirectives(*AbstractKotlinCompilerTest.defaultDirectiveContainers.toTypedArray())
        useDirectives(JvmEnvironmentConfigurationDirectives)
        useDirectives(TargetPlatformDirectives)
        useDirectives(ConfigurationDirectives)
        useDirectives(NativeEnvironmentConfigurationDirectives)
        useDirectives(JsEnvironmentConfigurationDirectives)

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.FULL_JDK
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
    open fun compileLibraryModule(module: TestModule, testServices: TestServices) {
        val javaFiles = module.writeJavaFiles()
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(
            module,
            CompilationStage.FIRST
        )
        val dependencies = module.allDependencies.map { outDirForModule(it.dependencyModule.name) }
        compilerConfiguration.addJvmClasspathRoots(dependencies)
        compilerConfiguration.addJavaSourceRoot(module.javaDir)

        // TODO: other platforms
        val configurationProvider = testServices.compilerConfigurationProvider
        val project = configurationProvider.getProject(module)
        val ktFiles = module.loadKtFiles(project)
        GenerationUtils.compileFiles(
            ktFiles,
            compilerConfiguration,
            ClassBuilderFactories.TEST,
            configurationProvider.getPackagePartProviderFactory(module)
        ).factory.apply {
            writeAllTo(module.outDir)
        }

        if (module.javaFiles.isEmpty())
            return

        val classpath = (dependencies + KtTestUtil.getAnnotationsJar() + module.outDir)
            .joinToString(File.pathSeparator) { it.absolutePath }
        val options = listOf(
            "-classpath", classpath,
            "-d", module.outDir.path
        )
        compileJavaFiles(javaFiles, options)
    }

    /**
     * Runs a positive test, asserting the actual output matches the expected output.
     */
    fun runTest(@TestDataFile path: String) {
        val (expected, actual) = loadTest(path)
        Assertions.assertEquals(expected, collectAllExceptions { actual() })
    }

    /**
     * Runs a negative test, asserting the actual output does not match the expected output.
     */
    fun runFailingTest(@TestDataFile path: String) {
        val (expected, actual) = loadTest(path)
        Assertions.assertNotEquals(expected, collectAllExceptions { actual() })
    }

    /**
     * Runs a negative test, asserting that the implementation throws a throwable
     * that is a subtype of [expectedThrowableType].
     *
     * The test fails if that particular exception is not thrown, e.g., by not throwing at all
     * or by throwing a different exception.
     *
     * @param path the path to the test file.
     * @param expectedThrowableType the type of the expected throwable. Defaults to [InternalKSPException].
     */
    fun runThrowingTest(@TestDataFile path: String, expectedThrowableType: KClass<*> = InternalKSPException::class) {
        val (_, run) = loadTest(path)
        try {
            run()
            Assertions.fail("Expected ${expectedThrowableType.simpleName ?: "null"} but the run was successful.")
        } catch (e: Throwable) {
            if (!e::class.isSubclassOf(expectedThrowableType)) {
                // Fail by rethrowing to get better feedback
                throw e
            }
            // Succeed by returning
        }
    }

    /**
     * Loads the located at test at [path].
     *
     * @return a [Pair] containing the expected results in the first entry
     * and in the second entry a lambda that runs the test and returns the actual results.
     * The expected results can be directly compared with the results of the lambda in the second entry.
     */
    private fun loadTest(@TestDataFile path: String): Pair<String, () -> String> {
        val testConfiguration = testConfiguration(path, configure)
        Disposer.register(disposable, testConfiguration.rootDisposable)
        val testServices = testConfiguration.testServices
        val moduleStructure = testConfiguration.moduleStructureExtractor.splitTestDataByModules(
            path,
            testConfiguration.directives,
        )
        testServices.registerArtifactsProvider(ArtifactsProvider())
        testServices.register(TestModuleStructure::class, moduleStructure)

        val mainModule = moduleStructure.modules.last()
        val libModules = moduleStructure.modules.dropLast(1)

        for (lib in libModules) {
            compileLibraryModule(lib, testServices)
        }
        val compilerConfigurationMain = testServices.compilerConfigurationProvider.getCompilerConfiguration(
            mainModule,
            CompilationStage.FIRST
        )
        compilerConfigurationMain.addJvmClasspathRoots(libModules.map { it.outDir })

        val fileContents = mainModule.files.first().originalFile.readLines()

        val processorArguments = parseProcessorArguments(fileContents)
        val processorClass = mkTestProcessorClass(parseTestProcessorName(fileContents))
        val testProcessor = mkProcessor(processorArguments, processorClass)

        val expected = parseExpectedOutput(fileContents)[apiFeatures]
            ?.joinToString("\n")
            ?: ""

        val actual = {
            runTest(
                testServices,
                mainModule,
                libModules,
                testProcessor
            ).joinToString("\n")
        }

        return expected to actual
    }

    private fun mkTestProcessorClass(testProcessorName: String): Class<*> =
        Class.forName("com.google.devtools.ksp.processor.$testProcessorName")

    private fun parseTestProcessorName(fileContents: List<String>): String = fileContents
        .single { it.startsWith(TEST_PROCESSOR) }
        .substringAfter(TEST_PROCESSOR)
        .trim()

    private fun parseProcessorArguments(fileContents: List<String>): List<String>? = fileContents
        .find { it.startsWith(PROCESSOR_INPUT) }
        ?.substringAfter(PROCESSOR_INPUT)
        ?.split(',')
        ?.map { it.trim() }

    /**
     * Given the test file content, [parseExpectedOutput] returns a map of expected test results/output based
     * on the [apiFeatures] configuration. Thus, given the feature toggle, the caller may index into the
     * returned map to obtain the expected test results.
     *
     * [parseExpectedOutput] removes directives such as `EXPECT ENABLE_NEW_FEATURES` and removes dangling
     * whitespace and comments. In other words, if `// MyExpectedOutput` is declared in the test file,
     * the value `"MyExpectedOutput"` is in the returned list for all configurations.
     */
    private fun parseExpectedOutput(fileContents: List<String>): Map<ApiFeatures, List<String>> {
        val rawExpectedOutput =
            fileContents
                .dropWhile { !it.startsWith(EXPECTED_RESULTS) }
                .drop(1)
                .takeWhile { !it.startsWith(EXPECTED_RESULTS_END) }

        return buildMap<ApiFeatures, MutableList<String>> {
            rawExpectedOutput.forEach { line ->
                if (line.startsWith(EXPECT_LINE)) {
                    // The expectation is combination of feature values
                    val featureConfigStrings = line
                        .drop(EXPECT_LINE.length)
                        .takeWhile { it != ':' }
                        .split(',')
                        .map { it.trim() }

                    val parsedFeatures = ApiFeaturesImpl.parse(featureConfigStrings)
                    getOrPut(parsedFeatures, ::mutableListOf)
                        .add(line.dropWhile { it != ':' }.drop(1).trim())
                } else {
                    // Add the expectation to all configurations
                    ApiFeaturesImpl.ALL_COMBINATIONS.forEach { apiFeatures ->
                        getOrPut(apiFeatures, ::mutableListOf)
                            .add(line.drop(COMMENT_TOKEN.length).trim())
                    }
                }
            }
        }
    }

    private fun mkProcessor(
        processorArguments: List<String>?,
        processorClass: Class<*>
    ): AbstractTestProcessor = if (processorArguments == null) {
        // Instantiate processor class with enableNewFeatures param
        processorClass
            .getDeclaredConstructor(Boolean::class.java)
            .newInstance(this.apiFeatures) as AbstractTestProcessor
    } else {
        // Instantiate parameterized processor class
        processorClass
            .getDeclaredConstructor(List::class.java, Boolean::class.java)
            .newInstance(processorArguments, this.apiFeatures) as AbstractTestProcessor
    }

    /**
     * Simple data class implementation of [ApiFeatures].
     *
     * The data class implements structural equality which is handy for comparing configurations
     * in the test setup.
     */
    private data class ApiFeaturesImpl(
        override val enableBackingFields: Boolean,
        override val enableContextParameters: Boolean
    ) : ApiFeatures {

        companion object {

            val ALL_COMBINATIONS = setOf(
                ApiFeaturesImpl(true, true),
                ApiFeaturesImpl(true, false),
                ApiFeaturesImpl(false, true),
                ApiFeaturesImpl(false, false),
            )

            val NEUTRAL = ApiFeaturesImpl(
                enableBackingFields = false,
                enableContextParameters = false
            )

            @JvmStatic
            fun combine(left: ApiFeatures, right: ApiFeatures): ApiFeaturesImpl =
                ApiFeaturesImpl(
                    enableBackingFields = left.enableBackingFields || right.enableBackingFields,
                    enableContextParameters = left.enableContextParameters || right.enableContextParameters
                )

            /**
             * Parses a string of the form `ENABLE_FEATURE = BOOLEAN` where whitespace is optional.
             * `ENABLE_FEATURE` is a screaming snake case version of the properties in [ApiFeatures]
             * and `BOOLEAN` is just the uppercase string representation of a boolean literal `true, false`.
             */
            @JvmStatic
            fun parse(str: String): ApiFeaturesImpl =
                str
                    .trim()
                    .split('=')
                    .map { it.trim() }
                    .let {
                        val expectedSize = 2
                        if (it.size != expectedSize) {
                            throw InternalKSPException(
                                message = "Expected list size to be exactly $expectedSize in ApiFeaturesImpl.parse. Was ${it.size}: $it",
                                location = NonExistLocation,
                                originatingClass = this.javaClass
                            )
                        }

                        // TODO: Enumerate the properties of [ApiFeatures]
                        //   and look up the property based on the string representation.
                        //   Doing so allows the parse function to automatically parse new features as they are added.

                        val lhs = it[0]
                        val rhs = it[1].lowercase().toBooleanStrict()
                        when (lhs) {
                            "ENABLE_BACKING_FIELDS" -> ApiFeaturesImpl(
                                enableBackingFields = rhs,
                                enableContextParameters = false
                            )

                            "ENABLE_CONTEXT_PARAMETERS" -> ApiFeaturesImpl(
                                enableBackingFields = false,
                                enableContextParameters = rhs,
                            )

                            else -> TODO("Not implemented: Throw InternalKSPException")
                        }
                    }

            @JvmStatic
            fun parse(strs: Collection<String>): ApiFeaturesImpl =
                // TODO: Report error when feature is assigned multiple values
                strs.map(::parse).fold(NEUTRAL, ::combine)
        }
    }
}

/**
 * Collects exception from all threads when running `block`.
 * Throws an [Exception] if any exception occurred.
 *
 * Note that function is not a perfect solution as it only catches exceptions
 * that happen during `block`.
 * Some threads may produce exceptions AFTER this function successfully returns,
 * but the purpose of this function is to help catch exceptions sometimes.
 */
internal fun <A> collectAllExceptions(block: () -> A): A {
    val exceptions = ConcurrentLinkedQueue<Throwable>()

    // Save original default exception handler
    val originalDefaultThreadHandler = Thread.getDefaultUncaughtExceptionHandler()

    // Override default handler and collect exceptions in `exceptions`.
    Thread.setDefaultUncaughtExceptionHandler { _, throwable -> exceptions.add(throwable) }

    // Run the block
    val result = block()

    // Flush the AWT event queue to process any pending events.
    // This helps catch exceptions from AWT/Swing components that might
    // occur asynchronously after the main block has completed.
    try {
        EventQueue.invokeAndWait { }
    } catch (e: Exception) {
        // If flushing the queue itself causes an error, catch it.
        exceptions.add(e)
    }

    // Restore original default exception handler
    Thread.setDefaultUncaughtExceptionHandler(originalDefaultThreadHandler)

    if (exceptions.isNotEmpty()) {
        val message = buildString {
            append("Failed with ")
            append(exceptions.size)
            appendLine(" uncaught errors:")
            exceptions.forEach { exception ->
                appendLine(exception)
                exception.stackTrace.forEach { appendLine(it.toString()) }
                appendLine()
                appendLine()
            }
        }
        throw Exception(message)
    }
    return result
}
