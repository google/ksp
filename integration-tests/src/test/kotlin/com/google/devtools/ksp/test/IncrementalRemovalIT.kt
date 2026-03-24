package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class IncrementalRemovalIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "incremental-removal",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    @Test
    fun testRemoveOutputs() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val k1 = "workload/src/main/kotlin/p1/K1.kt"

        gradleRunner.withArguments("run").build().let { result ->
            Assert.assertTrue(result.output.contains("result: generated"))
        }

        File(project.root, k1).writeText(
            "package p1\n\nclass K1\n\nclass Foo : Bar { override fun s() = \"crafted\" }\n"
        )
        gradleRunner.withArguments("run").build().let { result ->
            Assert.assertTrue(result.output.contains("result: crafted"))
        }

        project.restore(k1)
        gradleRunner.withArguments("run").build().let { result ->
            Assert.assertTrue(result.output.contains("result: generated"))
        }
    }
}
