package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class OnlyResourcesFileIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "only-resources-file")
        project.setup()
    }

    @Test
    fun test() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "jvmJar",
        ).build()
    }
}
