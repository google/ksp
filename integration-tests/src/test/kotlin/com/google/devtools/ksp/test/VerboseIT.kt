package com.google.devtools.ksp.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.jar.JarFile

@RunWith(Parameterized::class)
class VerboseIT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground", useKSP2 = useKSP2)

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
    fun testNoProvider() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        File(
            project.root,
            "test-processor/src/main/resources/META-INF/services/" +
                "com.google.devtools.ksp.processing.SymbolProcessorProvider"
        ).delete()
        gradleRunner.withArguments("build").buildAndFail().let { result ->
            Assert.assertTrue(result.output.contains("No providers found in processor classpath."))
        }
    }

    @Test
    fun testProviderAndRoundLogging() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("--debug", "clean", "build") { result ->
            Assert.assertTrue(
                result.output.contains(
                    "i: [ksp] loaded provider(s): [TestProcessorProvider, TestProcessorProvider2]"
                )
            )
            Assert.assertTrue(result.output.contains("v: [ksp] round 3 of processing"))
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
