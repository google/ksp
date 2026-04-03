package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import com.google.devtools.ksp.test.utils.assertContainsNonNullEntry
import com.google.devtools.ksp.test.utils.assertMergedConfigurationOutput
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarFile

class AndroidIT {
    @TempDir
    lateinit var tempDir: File

    lateinit var project: TemporaryTestProject

    @BeforeEach
    fun setup() {
        project = TemporaryTestProject(tempDir, "playground-android", "playground")
        project.setup()
    }

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace"
        ).build().let { result ->
            Assertions.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)

            val classesJar = File(
                project.root,
                "workload/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar"
            )
            JarFile(classesJar).use { jarFile ->
                jarFile.assertContainsNonNullEntry("Generated.class")
                jarFile.assertContainsNonNullEntry("hello/HELLO.class")
                jarFile.assertContainsNonNullEntry("com/example/AClassBuilder.class")
                jarFile.assertContainsNonNullEntry("com/example/BClassBuilder.class")
                jarFile.assertContainsNonNullEntry("com/example/AClass.class")
                jarFile.assertContainsNonNullEntry("com/example/BClass.class")
                jarFile.assertContainsNonNullEntry("TestProcessor.log")
                jarFile.assertContainsNonNullEntry("META-INF/proguard/builder-AClassBuilder.pro")
                jarFile.assertContainsNonNullEntry("META-INF/proguard/builder-BClassBuilder.pro")
            }

            val javaResDir = File(project.root, "workload/build/intermediates/java_res/debug/processDebugJavaRes/out")
            Assertions.assertTrue(File(javaResDir, "TestProcessor.log").exists())
            Assertions.assertTrue(File(javaResDir, "META-INF/proguard/builder-AClassBuilder.pro").exists())
            Assertions.assertTrue(File(javaResDir, "META-INF/proguard/builder-BClassBuilder.pro").exists())
            assertMergedConfigurationOutput(project, "-keep class com.example.AClassBuilder { *; }")
            assertMergedConfigurationOutput(project, "-keep class com.example.BClassBuilder { *; }")

            val outputs = result.output.lines()
            Assertions.assertTrue(
                "w: [ksp] [workload_debug] Mangled name for internalFun: internalFun\$workload_debug" in outputs
            )
            Assertions.assertTrue(
                "w: [ksp] [workload_release] Mangled name for internalFun: internalFun\$workload_release" in outputs
            )
        }
    }
}
