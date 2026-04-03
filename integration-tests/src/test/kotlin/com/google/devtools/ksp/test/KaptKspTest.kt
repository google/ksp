package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KaptKspTest {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "android-view-binding", "playground")
        project.setup()
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
            Assertions.assertTrue(kspTask.isNotEmpty())
            Assertions.assertTrue(kaptTask.isNotEmpty())
        }
    }
}
