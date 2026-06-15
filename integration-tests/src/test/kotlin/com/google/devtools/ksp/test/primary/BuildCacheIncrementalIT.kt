package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class BuildCacheIncrementalIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "buildcache-incremental",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    // See https://github.com/google/ksp/issues/2042 for details
    @Test
    fun testIncrementalBuildCache() {
        val buildCacheDir = File(project.root, "build-cache").absolutePath.replace(File.separatorChar, '/')
        File(project.root, "gradle.properties").appendText("\nbuildCacheDir=$buildCacheDir")
        File(project.root, "gradle.properties").appendText("\norg.gradle.caching=true")

        val gradleRunner = GradleRunner.create().withProjectDir(project.root).forwardOutput()
        val k1 = "workload/src/main/kotlin/p1/K1.kt"
        val k2 = "workload/src/main/kotlin/p1/K2.kt"
        val app = "workload/src/main/kotlin/p1/App.kt"

        // App depends on K1Generated
        File(project.root, app).writeText(
            """
            package p1
            class App {
                val k1Gen = K1Generated()
            }
            """.trimIndent()
        )

        // Build 1: Initial clean build. Should generate K1Generated.kt.
        println("--- BUILD 1 (Clean) ---")
        gradleRunner.withArguments("assemble", "--stacktrace").build()

        // Modify K1.kt to remove annotation. K1.kt is now dirty.
        // Since K1.kt is dirty, K1Generated.kt (associated with K1.kt) should NOT be restored.
        // And since K1.kt no longer has the annotation, K1Generated.kt should NOT be regenerated.
        // So K1Generated.kt should be gone.
        // App.kt depends on K1Generated, so the build should FAIL.
        File(project.root, k1).writeText(
            """
            package p1
            class K1
            """.trimIndent()
        )

        println("--- BUILD 2 (Incremental after modifying K1 to remove annotation) ---")
        // We expect this to fail because K1Generated is missing.
        // If it succeeds, it means K1Generated was incorrectly restored (bug).
        val result = gradleRunner.withArguments("assemble", "--stacktrace").buildAndFail()
        assert(result.output.contains("Unresolved reference 'K1Generated'") || result.output.contains("Unresolved reference: K1Generated")) {
            "Expected build to fail with Unresolved reference 'K1Generated', but got: ${result.output}"
        }
        println("--- BUILD 2 FAILED AS EXPECTED (K1Generated was correctly not restored) ---")


    }
}
