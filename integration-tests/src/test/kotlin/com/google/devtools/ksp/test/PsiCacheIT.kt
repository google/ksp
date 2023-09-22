package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PsiCacheIT(useK2: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("psi-cache", "test-processor", useK2 = useK2)

    @Test
    fun testPsiCache() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments("assemble").build()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "K2={0}")
        fun params() = listOf(arrayOf(true), arrayOf(false))
    }
}
