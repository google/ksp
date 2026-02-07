package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import com.google.devtools.ksp.test.utils.assertContainsNonNullEntry
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.*

class MultiplatformIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-mpp", "playground")

    @Test
    fun testJVM() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("mac", ignoreCase = true))
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild =
            gradleRunner.withArguments("--configuration-cache-problems=warn", "clean", "build").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-jvm-1.0-SNAPSHOT.jar")
        Assert.assertTrue(artifact.exists())

        JarFile(artifact).use { jarFile ->
            jarFile.assertContainsNonNullEntry("TestProcessor.log")
            jarFile.assertContainsNonNullEntry("Generated.class")
            jarFile.assertContainsNonNullEntry("META-INF/proguard/builder-AClassBuilder.pro")
            jarFile.assertContainsNonNullEntry("hello/HELLO.class")
            jarFile.assertContainsNonNullEntry("com/example/AClassBuilder.class")
            jarFile.assertContainsNonNullEntry("com/example/BClassBuilder.class")
            jarFile.assertContainsNonNullEntry("com/example/AClass.class")
        }
    }

    @Test
    fun testAndroid() {
        Assume.assumeFalse(System.getProperty("os.name").startsWith("mac", ignoreCase = true))
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild =
            gradleRunner.withArguments("--configuration-cache-problems=warn", "clean", "build").build()

        Assert.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val classesJar = File(
            project.root,
            "workload/build/intermediates/compile_library_classes_jar/androidMain/" +
                "bundleAndroidMainClassesToCompileJar/classes.jar"
        )
        Assert.assertTrue(classesJar.exists())

        JarFile(classesJar).use { jarFile ->
            jarFile.assertContainsNonNullEntry("com/example/AKt.class")
            jarFile.assertContainsNonNullEntry("com/example/AClassBuilder.class")
            jarFile.assertContainsNonNullEntry("com/example/BClassBuilder.class")
            jarFile.assertContainsNonNullEntry("com/example/AClass.class")
        }
    }
}
