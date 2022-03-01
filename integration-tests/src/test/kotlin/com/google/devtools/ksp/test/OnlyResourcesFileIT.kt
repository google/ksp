package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test

class OnlyResourcesFileIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("only-resources-file")

    @Test
    fun test() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "--configuration-cache-problems=warn",
            "jvmJar",
        ).build()
    }
}
