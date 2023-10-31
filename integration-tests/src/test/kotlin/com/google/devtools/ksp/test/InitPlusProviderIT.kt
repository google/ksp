package com.google.devtools.ksp.test

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
class InitPlusProviderIT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("init-plus-provider", useKSP2 = useKSP2)

    @Test
    fun testInitPlusProvider() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild = gradleRunner.withArguments("clean", "build").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-1.0-SNAPSHOT.jar")
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            Assert.assertTrue(jarFile.getEntry("TestProcessor.log").size > 0)
            Assert.assertTrue(jarFile.getEntry("HelloFromProvider.class").size > 0)
            Assert.assertTrue(jarFile.getEntry("GeneratedFromProvider.class").size > 0)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
