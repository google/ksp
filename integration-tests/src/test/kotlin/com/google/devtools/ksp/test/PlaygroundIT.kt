package com.google.devtools.ksp.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.jar.*

@RunWith(Parameterized::class)
class PlaygroundIT(val useK2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground", useK2 = useK2)

    private fun GradleRunner.buildAndCheck(vararg args: String, extraCheck: (BuildResult) -> Unit = {}) =
        buildAndCheckOutcome(*args, outcome = TaskOutcome.SUCCESS, extraCheck = extraCheck)

    private fun GradleRunner.buildAndCheckOutcome(
        vararg args: String,
        outcome: TaskOutcome,
        extraCheck: (BuildResult) -> Unit = {}
    ) {
        val result = this.withArguments(*args).build()

        Assert.assertEquals(outcome, result.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assert.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assert.assertTrue(jarFile.getEntry("hello/HELLO.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("g/G.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("com/example/AClassBuilder.class").size > 0)
        }

        extraCheck(result)
    }

    @Test
    fun testPlayground() {
        // FIXME: `clean` fails to delete files on windows.
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build")
        gradleRunner.buildAndCheck("clean", "build")
    }

    @Test
    fun testPlaygroundJDK8() {
        // FIXME: `clean` fails to delete files on windows.
        File(project.root, "test-processor/build.gradle.kts").appendText(
            """
            kotlin {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
               }
            }
            """.trimIndent()
        )
        File(project.root, "workload/build.gradle.kts").appendText(
            """
            kotlin {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
               }
            }
            """.trimIndent()
        )
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build")
        gradleRunner.buildAndCheck("clean", "build")
    }

    @Test
    fun testConfigurationOfConfiguration() {
        // FIXME: `clean` fails to delete files on windows.
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("8.0")
        gradleRunner.withArguments(":workload:dependencies", "--info").build().let {
            Assert.assertTrue(
                it.output.lines().filter { it.startsWith("The configuration :workload:ksp") }.isEmpty()
            )
        }
    }

    // TODO: add another plugin and see if it is blocked.
    // Or use a project that depends on a builtin plugin like all-open and see if the build fails
    @Test
    fun testBlockOtherCompilerPlugins() {
        // FIXME: `clean` fails to delete files on windows.
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  blockOtherCompilerPlugins = false\n}\n")
        gradleRunner.buildAndCheck("clean", "build")
        gradleRunner.buildAndCheck("clean", "build")
        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testAllowSourcesFromOtherPlugins() {
        Assume.assumeFalse(useK2)
        fun checkGBuilder() {
            val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")

            JarFile(artifact).use { jarFile ->
                Assert.assertTrue(jarFile.getEntry("g/GBuilder.class").size > 0)
            }
        }

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  allowSourcesFromOtherPlugins = true\n}\n")
        gradleRunner.buildAndCheck("clean", "build") { checkGBuilder() }
        gradleRunner.buildAndCheckOutcome("build", "--info", outcome = TaskOutcome.UP_TO_DATE) {
            Assert.assertEquals(TaskOutcome.UP_TO_DATE, it.task(":workload:kspKotlin")?.outcome)
            checkGBuilder()
        }
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
            Assert.assertEquals(TaskOutcome.FROM_CACHE, it.task(":workload:kspKotlin")?.outcome)
        }
    }

    @Test
    fun testAllWarningsAsErrors() {
        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  allWarningsAsErrors = true\n}\n")
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.withArguments("build").buildAndFail().let { result ->
            Assert.assertTrue(result.output.contains("This is a harmless warning."))
        }
    }

    // Compiler's test infra report this kind of error before KSP, so it is not testable there.
    @Test
    fun testNoFunctionName() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        fun buildAndFileAndCheck() {
            gradleRunner.withArguments("build").buildAndFail().let { result ->
                Assert.assertTrue(result.output.contains("Function declaration must have a name"))
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
            Assert.assertTrue(result.output.contains("kotlin.io.FileAlreadyExistsException"))
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

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assert.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assert.assertTrue(jarFile.getEntry("hello/HELLO.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("com/example/AClassBuilder.class").size > 0)
        }
        Assert.assertTrue(result.output.contains("w: Language version 2.0 is experimental"))
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

        Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assert.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assert.assertTrue(jarFile.getEntry("hello/HELLO.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("com/example/AClassBuilder.class").size > 0)
        }
        Assert.assertTrue(result.output.contains("w: Language version 2.0 is experimental"))
        project.restore(buildFile.path)
        project.restore(gradleProperties.path)
    }

    @Test
    fun testVersions() {
        Assume.assumeFalse(useK2)
        val kotlinCompile = "org.jetbrains.kotlin.gradle.tasks.KotlinCompile"
        val buildFile = File(project.root, "workload/build.gradle.kts")
        buildFile.appendText("\ntasks.withType<$kotlinCompile> {")
        buildFile.appendText("\n    kotlinOptions.apiVersion = \"1.5\"")
        buildFile.appendText("\n    kotlinOptions.languageVersion = \"1.5\"")
        buildFile.appendText("\n}")

        val kotlinVersion = System.getProperty("kotlinVersion").split('-').first()
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build") { result ->
            Assert.assertTrue(result.output.contains("language version: 1.5"))
            Assert.assertTrue(result.output.contains("api version: 1.5"))
            Assert.assertTrue(result.output.contains("compiler version: $kotlinVersion"))
        }
        project.restore(buildFile.path)
    }

    @Test
    fun testExcludeProcessor() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  excludeProcessor(\"TestProcessorProvider\")\n")
        File(project.root, "workload/build.gradle.kts")
            .appendText("\n  excludeProcessor(\"NotMatchingAnything\")\n}\n")
        gradleRunner.withArguments("build").buildAndFail().let {
            Assert.assertEquals(TaskOutcome.SUCCESS, it.task(":workload:kspKotlin")?.outcome)
            Assert.assertEquals(TaskOutcome.FAILED, it.task(":workload:compileKotlin")?.outcome)
            Assert.assertTrue("Unresolved reference: AClassBuilder" in it.output)
        }
        gradleRunner.withArguments("build").buildAndFail().let {
            Assert.assertEquals(TaskOutcome.UP_TO_DATE, it.task(":workload:kspKotlin")?.outcome)
            Assert.assertEquals(TaskOutcome.FAILED, it.task(":workload:compileKotlin")?.outcome)
            Assert.assertTrue("Unresolved reference: AClassBuilder" in it.output)
        }

        project.restore("workload/build.gradle.kts")
        File(project.root, "workload/build.gradle.kts")
            .appendText("\nksp {\n  excludeProcessor(\"DoNotMatch\")\n}\n")
        gradleRunner.buildAndCheck("build")

        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testJvmPlatformInfo() {
        val kotlinCompile = "org.jetbrains.kotlin.gradle.tasks.KotlinCompile"
        val buildFile = File(project.root, "workload/build.gradle.kts")
        buildFile.appendText("\ntasks.withType<$kotlinCompile> {")
        buildFile.appendText("\n    kotlinOptions.freeCompilerArgs += \"-Xjvm-default=all\"")
        buildFile.appendText("\n}")

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("clean", "build") { result ->
            Assert.assertTrue(result.output.contains("platform: JVM"))
            Assert.assertTrue(result.output.contains("jvm default mode: all"))
        }
        project.restore(buildFile.path)
    }

    @Test
    fun testProjectExtensionCompilerOptions() {
        Assume.assumeFalse(useK2)
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
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
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("8.0")
        gradleRunner.withArguments("clean", "build").buildAndFail().let { result ->
            Assert.assertTrue(
                result.output.contains(
                    "'compileJava' task (current target is 11) and 'kspKotlin' " +
                        "task (current target is 20) jvm target compatibility should be set to the same Java version."
                )
            )
        }
        project.restore(buildFile.path)
        project.restore(properties.path)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "K2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
