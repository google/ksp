package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class KaptKspTest(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "android-view-binding",
        "playground",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    @Test
    fun testPlaygroundAndroid() {
        val buildFile = File(project.root, "app/build.gradle.kts")
        val content = buildFile.readText()
        val newContent = content.replace("kotlin(\"android\")", "kotlin(\"android\")\n kotlin(\"kapt\")\n")
        buildFile.writeText(newContent)
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "clean",
            ":app:testDebugUnitTest",
            "--configuration-cache-problems=warn",
            "--info",
            "--stacktrace"
        ).build().let { result ->
            val output = result.output.lines()
            val kspTask = output.filter { it.contains(":app:kspDebugKotlin") }
            val kaptTask = output.filter { it.contains(":app:kaptDebugKotlin") }
            Assert.assertTrue(kspTask.isNotEmpty())
            Assert.assertTrue(kaptTask.isNotEmpty())
        }
    }
}
