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

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val k1 = "workload/src/main/kotlin/p1/K1.kt"
        val k2 = "workload/src/main/kotlin/p1/K2.kt"

        gradleRunner.withArguments("assemble", "--stacktrace").build()

        File(project.root, k2).writeText(
            "package p1\n\n@MyAnnotation\nclass K2\n"
        )
        gradleRunner.withArguments("assemble", "--stacktrace").build()

        File(project.root, k2).delete()
        gradleRunner.withArguments("assemble", "--stacktrace").build()

        File(project.root, k1).writeText(
            "package p1\n\nclass K1(val foo: String)\n"
        )
        gradleRunner.withArguments("assemble", "--stacktrace").build()
    }

    // See https://github.com/google/ksp/issues/2854 for details
    @Test
    fun testIncrementalBuildCacheStaleOutputsPurged() {
        val buildCacheDir = File(project.root, "build-cache").absolutePath.replace(File.separatorChar, '/')
        File(project.root, "gradle.properties").appendText("\nbuildCacheDir=$buildCacheDir")
        File(project.root, "gradle.properties").appendText("\norg.gradle.caching=true")

        val gradleRunner = GradleRunner.create().withProjectDir(project.root).forwardOutput()
        val k1 = "workload/src/main/kotlin/p1/K1.kt"
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

        // We expect this to fail because K1Generated is missing.
        // If it succeeds, it means K1Generated was incorrectly restored (bug).
        val result = gradleRunner.withArguments("assemble", "--stacktrace").buildAndFail()
        assert(
            result.output.contains("Unresolved reference 'K1Generated'") ||
                result.output.contains("Unresolved reference: K1Generated")
        ) {
            "Expected build to fail with Unresolved reference 'K1Generated', but got: ${result.output}"
        }
    }

    // See https://github.com/google/ksp/issues/2854 for details
    @Test
    fun testIncrementalBuildCacheCleanOutputsPreserved() {
        val buildCacheDir = File(project.root, "build-cache").absolutePath.replace(File.separatorChar, '/')
        File(project.root, "gradle.properties").appendText("\nbuildCacheDir=$buildCacheDir")
        File(project.root, "gradle.properties").appendText("\norg.gradle.caching=true")

        val gradleRunner = GradleRunner.create().withProjectDir(project.root).forwardOutput()
        val k1 = "workload/src/main/kotlin/p1/K1.kt"
        val k2 = "workload/src/main/kotlin/p1/K2.kt"

        // Create K2.kt with annotation
        File(project.root, k2).writeText(
            """
            package p1
            @MyAnnotation
            class K2
            """.trimIndent()
        )

        // K1 depends on K2Generated (which is generated from K2.kt)
        File(project.root, k1).writeText(
            """
            package p1
            @MyAnnotation
            class K1 {
                val k2Gen: K2Generated? = null
            }
            """.trimIndent()
        )

        // Build 1: Initial clean build. Should generate K1Generated.kt and K2Generated.kt.
        gradleRunner.withArguments("assemble", "--stacktrace").build()

        // Modify K1.kt but KEEP annotation and dependency. K1.kt is dirty, K2.kt is clean.
        // Since K2.kt is clean, K2Generated.kt should be preserved/restored from cache.
        // K1.kt depends on K2Generated, so the build should SUCCEED.
        File(project.root, k1).writeText(
            """
            package p1
            @MyAnnotation
            class K1 {
                val k2Gen: K2Generated? = null
                val change = 1
            }
            """.trimIndent()
        )

        // We expect this to succeed because K2Generated should be preserved.
        // If it fails with "Unresolved reference 'K2Generated'", it means K2Generated was lost (bug).
        gradleRunner.withArguments("assemble", "--stacktrace").build()
    }
}
