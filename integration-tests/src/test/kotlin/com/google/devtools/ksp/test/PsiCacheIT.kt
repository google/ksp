package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test

class PsiCacheIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("psi-cache", "test-processor")

    @Test
    fun testPsiCache() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble").build()
    }
}
