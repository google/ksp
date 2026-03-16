package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import com.google.devtools.ksp.test.utils.assertContainsNonNullEntry
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.*

class PlaygroundIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "playground")
        project.setup()
    }

    private fun GradleRunner.buildAndCheck(vararg args: String, extraCheck: (BuildResult) -> Unit = {}) =
        buildAndCheckOutcome(*args, outcome = TaskOutcome.SUCCESS, extraCheck = extraCheck)

    private fun GradleRunner.buildAndCheckOutcome(
        vararg args: String,
        outcome: TaskOutcome,
        extraCheck: (BuildResult) -> Unit = {}
    ) {
        val result = this.withArguments(*args).build()

        Assertions.assertEquals(outcome, result.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assertions.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            jarFile.assertContainsNonNullEntry("TestProcessor.log")
            jarFile.assertContainsNonNullEntry("Generated.class")
            jarFile.assertContainsNonNullEntry("META-INF/proguard/builder-AClassBuilder.pro")
            jarFile.assertContainsNonNullEntry("hello/HELLO.class")
            jarFile.assertContainsNonNullEntry("g/G.class")
            jarFile.assertContainsNonNullEntry("com/example/AClassBuilder.class")
            jarFile.assertContainsNonNullEntry("com/example/BClassBuilder.class")
            jarFile.assertContainsNonNullEntry("com/example/AClass.class")
            jarFile.assertContainsNonNullEntry("com/example/BClass.class")
        }

        extraCheck(result)
    }

    @Test
    fun testPlayground() {
        // FIXME: `clean` fails to delete files on windows.
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build")
        gradleRunner.buildAndCheck("clean", "build")
    }

    @Test
    fun testPlaygroundJDK8() {
        // FIXME: `clean` fails to delete files on windows.
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))

        File(project.root, "test-processor/build.gradle.kts").appendText(
            """
            kotlin {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_9)
               }
            }
            """.trimIndent()
        )
        File(project.root, "workload/build.gradle.kts").appendText(
            """
            kotlin {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_9)
               }
            }
            """.trimIndent()
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build")
        gradleRunner.buildAndCheck("clean", "build")
    }

    @Test
    fun testConfigurationOfConfiguration() {
        // FIXME: `clean` fails to delete files on windows.
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.1.0")
        gradleRunner.withArguments(":workload:dependencies", "--info").build().let { result ->
            Assertions.assertTrue(
                result.output.lines().none { it.startsWith("The configuration :workload:ksp") }
            )
        }
    }

    // TODO: add another plugin and see if it is blocked.
    // Or use a project that depends on a builtin plugin like all-open and see if the build fails
    @Test
    fun testBlockOtherCompilerPlugins() {
        // FIXME: `clean` fails to delete files on windows.
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  blockOtherCompilerPlugins = false\n}\n")
        gradleRunner.buildAndCheck("clean", "build")
        gradleRunner.buildAndCheck("clean", "build")
        project.restore("workload/build.gradle.kts")
    }

    /** Regression test for https://github.com/google/ksp/issues/518. */
    @Test
    fun testBuildWithConfigureOnDemand() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("--configure-on-demand", ":workload:build")
    }

    @Test
    fun testBuildCache() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        // The first build can be FROM_CACHE or SUCCESS, and we only care about the second build.
        gradleRunner.buildAndCheck("--build-cache", ":workload:clean", "build")
        gradleRunner.buildAndCheck("--build-cache", ":workload:clean", "build") {
            Assertions.assertEquals(TaskOutcome.FROM_CACHE, it.task(":workload:kspKotlin")?.outcome)
        }
    }

    @Test
    fun testAllWarningsAsErrors() {
        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  allWarningsAsErrors = true\n}\n")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("build").buildAndFail().let { result ->
            Assertions.assertTrue(result.output.contains("This is a harmless warning."))
        }
    }

    // Compiler's test infra report this kind of error before KSP, so it is not testable there.
    @Test
    fun testNoFunctionName() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        fun buildAndFileAndCheck() {
            gradleRunner.withArguments("build").buildAndFail().let { result ->
                Assertions.assertTrue(result.output.contains("Function declaration must have a name"))
            }
        }

        File(project.root, "workload/src/main/java/com/example/A.kt").appendText("\n{}\n")
        buildAndFileAndCheck()
        project.restore("workload/src/main/java/com/example/A.kt")

        File(project.root, "workload/src/main/java/com/example/A.kt").appendText("\nfun() = {0}\n")
        buildAndFileAndCheck()
        project.restore("workload/src/main/java/com/example/A.kt")
    }

    @Test
    fun testRewriteFile() {
        File(
            project.root,
            "test-processor/src/main/resources/META-INF/services/" +
                "com.google.devtools.ksp.processing.SymbolProcessorProvider"
        ).writeText("RewriteProcessorProvider")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("build").buildAndFail().let { result ->
            Assertions.assertTrue(result.output.contains("kotlin.io.FileAlreadyExistsException"))
        }
    }

    @Test
    fun testFirPreview() {
        val buildFile = File(project.root, "workload/build.gradle.kts")
        // K2 enables HMPP even on JVM only project, and is not compatible with copy task for source.
        // Disable copy task check for K2 tests, it does not impact KSP itself.
        val buildFileContent = buildFile.readLines().dropLast(9)
        buildFile.writeText("")
        buildFileContent.forEach {
            buildFile.appendText("$it\n")
        }
        buildFile.appendText(
            """
            kotlin {
                sourceSets.all {
                    languageSettings.apply {
                        languageVersion = "2.0"
                    }
                }
            }
            """.trimIndent()
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments("clean", "build").build()

        Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assertions.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assertions.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assertions.assertTrue(jarFile.getEntry("hello/HELLO.class").size > 0)
            Assertions.assertTrue(jarFile.getEntry("com/example/AClassBuilder.class").size > 0)
        }
        project.restore(buildFile.path)
    }

    @Test
    fun testFirPreviewWithUseK2() {
        val gradleProperties = File(project.root, "gradle.properties")
        gradleProperties.appendText("\nkotlin.useK2=true")
        val buildFile = File(project.root, "workload/build.gradle.kts")
        // K2 enables HMPP even on JVM only project, and is not compatible with copy task for source.
        // Disable copy task check for K2 tests, it does not impact KSP itself.
        val buildFileContent = buildFile.readLines().dropLast(9)
        buildFile.writeText("")
        buildFileContent.forEach {
            buildFile.appendText("$it\n")
        }
        buildFile.appendText(
            """
            kotlin {
                sourceSets.all {
                    languageSettings.apply {
                        languageVersion = "2.0"
                    }
                }
            }
            """.trimIndent()
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments("clean", "build").build()

        Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assertions.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assertions.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assertions.assertTrue(jarFile.getEntry("hello/HELLO.class").size > 0)
            Assertions.assertTrue(jarFile.getEntry("com/example/AClassBuilder.class").size > 0)
        }
        project.restore(buildFile.path)
        project.restore(gradleProperties.path)
    }

    @Test
    fun testVersions() {
        val buildFile = File(project.root, "workload/build.gradle.kts")
        buildFile.appendText(
            """
            kotlin {
              compilerOptions {
                apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
                languageVersion.set(compilerOptions.apiVersion)
               }
            }
            """.trimIndent()
        )

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build") { result ->
            Assertions.assertTrue(result.output.contains("language version: 1.9"))
            Assertions.assertTrue(result.output.contains("api version: 1.9"))
            val expectedKspVersion = "2.0"
            Assertions.assertTrue(result.output.contains("ksp version: $expectedKspVersion"))
        }
        project.restore(buildFile.path)
    }

    @Test
    fun testInternalJdkVersion() {
        System.setProperty("java.version", "17-internal")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.buildAndCheck("clean", "build")
    }

    @Test
    fun testExcludeProcessor() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  excludeProcessor(\"TestProcessorProvider\")\n")
        File(project.root, "workload/build.gradle.kts")
            .appendText("\n  excludeProcessor(\"NotMatchingAnything\")\n}\n")
        gradleRunner.withArguments("build").buildAndFail().let {
            Assertions.assertEquals(TaskOutcome.SUCCESS, it.task(":workload:kspKotlin")?.outcome)
            Assertions.assertEquals(TaskOutcome.FAILED, it.task(":workload:compileKotlin")?.outcome)
            Assertions.assertTrue("Unresolved reference 'AClassBuilder'" in it.output)
        }
        gradleRunner.withArguments("build").buildAndFail().let {
            Assertions.assertEquals(TaskOutcome.UP_TO_DATE, it.task(":workload:kspKotlin")?.outcome)
            Assertions.assertEquals(TaskOutcome.FAILED, it.task(":workload:compileKotlin")?.outcome)
            Assertions.assertTrue("Unresolved reference 'AClassBuilder'" in it.output)
        }

        project.restore("workload/build.gradle.kts")
        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  excludeProcessor(\"DoNotMatch\")\n}\n")
        gradleRunner.buildAndCheck("build")

        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testJvmPlatformInfo() {
        val buildFile = File(project.root, "workload/build.gradle.kts")
        buildFile.appendText(
            """
            |tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            |    compilerOptions {
            |        jvmDefault.set(org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode.NO_COMPATIBILITY)
            |    }
            |}
            |""".trimMargin()
        )

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build") { result ->
            Assertions.assertTrue(result.output.contains("platform: JVM"))
            Assertions.assertTrue(result.output.contains("jvm default mode: no-compatibility"))
        }
        project.restore(buildFile.path)
    }

    @Test
    fun testProjectExtensionCompilerOptions() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val properties = File(project.root, "gradle.properties")
        properties.writeText(
            properties.readText().replace(
                "kotlin.jvm.target.validation.mode=warning",
                "kotlin.jvm.target.validation.mode=error"
            )
        )
        val buildFile = File(project.root, "workload/build.gradle.kts")
        buildFile.appendText(
            """
            kotlin {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_20)
               }
            }
            """.trimIndent()
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.1.0")
        gradleRunner.withArguments("clean", "build").buildAndFail().let { result ->
            Assertions.assertTrue(
                result.output.contains("Inconsistent JVM-target compatibility detected for tasks")
            )
        }
        project.restore(buildFile.path)
        project.restore(properties.path)
    }

    @Test
    fun testProgressiveMode() {
        val buildFile = File(project.root, "workload/build.gradle.kts")
        buildFile.appendText(
            """
            kotlin {
                compilerOptions {
                    progressiveMode.value(true)
               }
            }
            """.trimIndent()
        )
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.1.0")
        gradleRunner.withArguments("clean", "build").build().let {
            Assertions.assertFalse(
                it.output.contains(
                    "'-progressive' is meaningful only for the latest language version"
                )
            )
        }
        project.restore(buildFile.path)
    }

    @Test
    fun testModuleName() {
        File(project.root, "workload/build.gradle.kts").createNewFile()
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("build").build().let { result ->
            Assertions.assertTrue(result.output.contains("Module name is workload"))
        }
    }

    @Test
    fun testEmpty() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/src/main/java/Empty.kt").appendText("\n\n")
        gradleRunner.withArguments("clean", "assemble", "-Pksp.incremental.log=false").build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:assemble")?.outcome)
        }
    }

    @Test
    fun testGeneratedBinaryClass() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        File(project.root, "workload/src/main/java/NeedGenerated.kt").appendText("val v = BinaryClass()\n")
        File(
            project.root,
            "test-processor/src/main/resources/META-INF/services/" +
                "com.google.devtools.ksp.processing.SymbolProcessorProvider"
        ).appendText("BinaryGenProcessorProvider")
        gradleRunner.buildAndCheck("clean", "build") {
            val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
            Assertions.assertTrue(artifact.exists())

            JarFile(artifact).use { jarFile ->
                Assertions.assertTrue(jarFile.getEntry("BinaryClass.class").size > 0)
            }
        }
    }

    @Test
    fun testNoProvider() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        File(
            project.root,
            "test-processor/src/main/resources/META-INF/services/" +
                "com.google.devtools.ksp.processing.SymbolProcessorProvider"
        ).delete()
        gradleRunner.withArguments("build").buildAndFail().let { result ->
            Assertions.assertTrue(result.output.contains("No providers found in processor classpath."))
        }
    }

    @Test
    fun testProviderAndRoundLogging() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("--debug", "clean", "build") { result ->
            Assertions.assertTrue(
                result.output.contains(
                    "i: [ksp] loaded provider(s): [TestProcessorProvider, TestProcessorProvider2]"
                )
            )
            Assertions.assertTrue(result.output.contains("v: [ksp] round 3 of processing"))
        }
    }
}
