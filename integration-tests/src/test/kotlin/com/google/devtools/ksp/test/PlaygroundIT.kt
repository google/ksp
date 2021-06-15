package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.*

class PlaygroundIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground")

    private fun GradleRunner.buildAndCheck(vararg flags: String) {
        val resultCleanBuild = this.withArguments(*flags, "clean", "build").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assert.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assert.assertTrue(jarFile.getEntry("hello/HELLO.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("com/example/AClassBuilder.class").size > 0)
        }
    }

    @Test
    fun testPlayground() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck()
    }

    // TODO: add another plugin and see if it is blocked.
    // Or use a project that depends on a builtin plugin like all-open and see if the build fails
    @Test
    fun testBlockOtherCompilerPlugins() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        File(project.root, "workload/build.gradle.kts").appendText("\nksp {\n  blockOtherCompilerPlugins = true\n}\n")
        gradleRunner.buildAndCheck()
        project.restore("workload/build.gradle.kts")
    }

    @Test
    fun testBuildWithConfigurationCache() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        gradleRunner.buildAndCheck("--configuration-cache")
    }
}
