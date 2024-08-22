package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class IncrementalRemoval2IT(useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("incremental-removal2", useKSP2 = useKSP2)

    @Test
    fun testRemoveOutputs() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val k2 = "workload/src/main/kotlin/p1/K2.kt"

        gradleRunner.withArguments("run").build().let { result ->
            Assert.assertTrue(result.output.contains("Written: K1.kt"))
            Assert.assertTrue(result.output.contains("Input: K1.kt"))
            Assert.assertTrue(result.output.contains("Input: K2.kt"))
        }

        File(project.root, k2).delete()
        gradleRunner.withArguments("run").build().let { result ->
            Assert.assertTrue(result.output.contains("Written: K1.kt"))
            Assert.assertTrue(result.output.contains("Input: K1.kt"))
            Assert.assertFalse(result.output.contains("Input: K2.kt"))
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
