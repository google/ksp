package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import com.google.devtools.ksp.test.utils.assertContainsNonNullEntry
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.*

class MultiplatformIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "playground-mpp")
        project.setup()
    }

    @Test
    fun testJVM() {
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("mac", ignoreCase = true))
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild =
            gradleRunner.withArguments("--configuration-cache-problems=warn", "clean", "build").build()

        Assertions.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val artifact = File(project.root, "workload/build/libs/workload-jvm-1.0-SNAPSHOT.jar")
        Assertions.assertTrue(artifact.exists())

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
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("mac", ignoreCase = true))
        Assumptions.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        val resultCleanBuild =
            gradleRunner.withArguments("--configuration-cache-problems=warn", "clean", "build").build()

        Assertions.assertEquals(TaskOutcome.SUCCESS, resultCleanBuild.task(":workload:build")?.outcome)

        val classesJar = File(
            project.root,
            "workload/build/intermediates/compile_library_classes_jar/androidMain/" +
                "bundleAndroidMainClassesToCompileJar/classes.jar"
        )
        Assertions.assertTrue(classesJar.exists())

        JarFile(classesJar).use { jarFile ->
            jarFile.assertContainsNonNullEntry("com/example/AKt.class")
            jarFile.assertContainsNonNullEntry("com/example/AClassBuilder.class")
            jarFile.assertContainsNonNullEntry("com/example/BClassBuilder.class")
            jarFile.assertContainsNonNullEntry("com/example/AClass.class")
        }
    }
}
