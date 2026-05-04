package com.google.devtools.ksp.test.primary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import com.google.devtools.ksp.test.utils.assertContainsNonNullEntry
import com.google.devtools.ksp.test.utils.assertMergedConfigurationOutput
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.jar.JarFile

@RunWith(Parameterized::class)
class AndroidIT(experimentalPsiResolution: Boolean) {

    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "playground-android",
        "playground",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    @Test
    fun testPlaygroundAndroid() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace"
        ).build().let { result ->
            Assert.assertEquals(TaskOutcome.SUCCESS, result.task(":workload:build")?.outcome)

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
            Assert.assertTrue(File(javaResDir, "TestProcessor.log").exists())
            Assert.assertTrue(File(javaResDir, "META-INF/proguard/builder-AClassBuilder.pro").exists())
            Assert.assertTrue(File(javaResDir, "META-INF/proguard/builder-BClassBuilder.pro").exists())
            assertMergedConfigurationOutput(project, "-keep class com.example.AClassBuilder { *; }")
            assertMergedConfigurationOutput(project, "-keep class com.example.BClassBuilder { *; }")

            val outputs = result.output.lines()
            assert("w: [ksp] [workload_debug] Mangled name for internalFun: internalFun\$workload_debug" in outputs)
            assert("w: [ksp] [workload_release] Mangled name for internalFun: internalFun\$workload_release" in outputs)
        }
    }
}
