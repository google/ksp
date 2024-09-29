package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class KotlinInjectIT(val useKSP2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("kotlin-inject", useKSP2 = useKSP2)

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

        // Triggers the caching issue
        setup(shouldFail = false)
        gradleRunner.withArguments("compileKotlinJvm")
            .buildAndFail().let { result ->
                println("OUTPUT START")
                println(result.output)
                Assert.assertTrue(result.output.contains("id-to-file.tab] is already registered"))
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "KSP2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
