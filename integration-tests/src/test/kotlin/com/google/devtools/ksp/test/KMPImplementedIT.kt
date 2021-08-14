package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.*

class KMPImplementedIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("kmp")

    private fun verify(jarName: String, contents: List<String>) {
        val artifact = File(project.root, jarName)
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            contents.forEach {
                Assert.assertTrue(jarFile.getEntry(it).size > 0)
            }
        }
    }

    private fun verifyKexe(path: String) {
        val artifact = File(project.root, path)
        Assert.assertTrue(artifact.exists())
        Assert.assertTrue(artifact.readBytes().size > 0)
    }

    @Test
    fun testAll() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        // KotlinNative doesn't support configuration cache yet.
        val resultCleanBuild = gradleRunner.withArguments("--configuration-cache-problems=warn", "clean", "build")
            .build()

        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        verify(
            "workload/build/libs/workload-jvm-1.0-SNAPSHOT.jar",
            listOf(
                "com/example/Foo.class"
            )
        )

        verify(
            "workload/build/libs/workload-js-1.0-SNAPSHOT.jar",
            listOf(
                "playground-workload.js"
            )
        )

        verify(
            "workload/build/libs/workload-metadata-1.0-SNAPSHOT.jar",
            listOf(
                "com/example/Foo.kotlin_metadata"
            )
        )

        verifyKexe("workload/build/bin/linuxX64/debugExecutable/workload.kexe")
        verifyKexe("workload/build/bin/linuxX64/releaseExecutable/workload.kexe")
        verifyKexe("workload/build/bin/androidNativeX64/debugExecutable/workload.so")
        verifyKexe("workload/build/bin/androidNativeX64/releaseExecutable/workload.so")
        verifyKexe("workload/build/bin/androidNativeArm64/debugExecutable/workload.so")
        verifyKexe("workload/build/bin/androidNativeArm64/releaseExecutable/workload.so")
    }
}
