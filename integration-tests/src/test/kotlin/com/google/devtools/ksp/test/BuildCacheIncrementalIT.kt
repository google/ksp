package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import java.io.File

class BuildCacheIncrementalIT() {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("buildcache-incremental")

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
}
