package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class JavaNestedClassIT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("javaNestedClass", useKSP2 = useKSP2)

    @Test
    fun testJavaNestedClass() {

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild = gradleRunner.withArguments("clean", "build").build()
        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
