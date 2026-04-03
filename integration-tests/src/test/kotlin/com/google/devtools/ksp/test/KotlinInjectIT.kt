package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KotlinInjectIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "kotlin-inject")
        project.setup()
    }

    @Test
    fun triggerException() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val path = "workload/src/commonMain/kotlin/AppComponent.kt"
        val file = File(project.root, path)

        fun setup(shouldFail: Boolean) {
            project.restore(path)

            // kotlin-inject will complain that Component classes can't be private
            if (shouldFail) {
                file
                    .apply {
                        writeText(
                            file.readText()
                                .replace("abstract class AppComponent", "private abstract class AppComponent")
                        )
                    }
            }
        }

        // Start the kotlin daemon?
        setup(shouldFail = false)
        gradleRunner.withArguments("compileKotlinJvm").build()

        // Make a processor fail
        setup(shouldFail = true)
        gradleRunner.withArguments("compileKotlinJvm").buildAndFail()

        // Should not trigger the caching issue
        setup(shouldFail = false)
        gradleRunner.withArguments("compileKotlinJvm").build()
    }
}
