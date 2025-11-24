package com.google.devtools.ksp.test

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import com.google.devtools.ksp.test.utils.assertContainsNonNullEntry
import com.google.devtools.ksp.test.utils.assertMergedConfigurationOutput
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.jar.JarFile

class AndroidBuiltInKotlinIT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-builtinkotlin", "playground")

    @Test
    fun testPlaygroundAndroidWithBuiltInKotlin() {
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
            }

            val javaResDir = File(project.root, "workload/build/intermediates/java_res/debug/processDebugJavaRes/out")
            Assert.assertTrue(File(javaResDir, "TestProcessor.log").exists())
            Assert.assertTrue(File(javaResDir, "META-INF/proguard/builder-AClassBuilder.pro").exists())
            Assert.assertTrue(File(javaResDir, "META-INF/proguard/builder-BClassBuilder.pro").exists())
            assertMergedConfigurationOutput(project, "-keep class com.example.AClassBuilder { *; }")
            assertMergedConfigurationOutput(project, "-keep class com.example.BClassBuilder { *; }")

            val outputs = result.output.lines()
            assert("w: [ksp] [workload] Mangled name for internalFun: internalFun\$workload" in outputs)
            assert("w: [ksp] [workload] Mangled name for internalFun: internalFun\$workload" in outputs)
        }
    }

    @Test
    fun testPlaygroundAndroidWithBuiltInKotlinAGP90BelowAlpha14() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.0.0")

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-alpha05")

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace"
        ).buildAndFail().let { result ->
            Assert.assertTrue(
                result.output.contains(
                    "KSP is not compatible with Android Gradle Plugin's built-in Kotlin prior to AGP " +
                        "version 9.0.0-alpha14. Please upgrade to AGP 9.0.0-alpha14 or alternatively disable " +
                        "built-in kotlin by adding android.builtInKotlin=false and android.newDsl=false to " +
                        "gradle.properties and apply kotlin(\"android\") plugin"
                )
            )
        }
    }

    @Test
    fun testPlaygroundAndroidWithBuiltInKotlinAGP90AboveAlpha14() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.1.0")

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-alpha14")

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
            }

            val javaResDir = File(project.root, "workload/build/intermediates/java_res/debug/processDebugJavaRes/out")
            Assert.assertTrue(File(javaResDir, "TestProcessor.log").exists())
            Assert.assertTrue(File(javaResDir, "META-INF/proguard/builder-AClassBuilder.pro").exists())
            Assert.assertTrue(File(javaResDir, "META-INF/proguard/builder-BClassBuilder.pro").exists())
            assertMergedConfigurationOutput(project, "-keep class com.example.AClassBuilder { *; }")
            assertMergedConfigurationOutput(project, "-keep class com.example.BClassBuilder { *; }")

            val outputs = result.output.lines()
            assert("w: [ksp] [workload] Mangled name for internalFun: internalFun\$workload" in outputs)
            assert("w: [ksp] [workload] Mangled name for internalFun: internalFun\$workload" in outputs)
        }
    }

    @Test
    fun testPlaygroundAndroidWithBuiltInKotlinProjectIsolationEnabled() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("9.1.0")

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-alpha14")
        File(project.root, "gradle.properties").appendText("\nkotlinVersion=2.3.0-Beta2")

        // override AGP's bundled kotlin gradle plugin version
        File(project.root, "workload/build.gradle.kts").appendText(
            """
                buildscript {
                    dependencies {
                        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0-Beta2")
                    }
                }
            """.trimIndent()
        )

        gradleRunner.withArguments(
            "clean", "build", "minifyReleaseWithR8", "--configuration-cache", "--info", "--stacktrace",
            "-Dorg.gradle.unsafe.isolated-projects=true"
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
            }

            val javaResDir = File(project.root, "workload/build/intermediates/java_res/debug/processDebugJavaRes/out")
            Assert.assertTrue(File(javaResDir, "TestProcessor.log").exists())
            Assert.assertTrue(File(javaResDir, "META-INF/proguard/builder-AClassBuilder.pro").exists())
            Assert.assertTrue(File(javaResDir, "META-INF/proguard/builder-BClassBuilder.pro").exists())
            assertMergedConfigurationOutput(project, "-keep class com.example.AClassBuilder { *; }")
            assertMergedConfigurationOutput(project, "-keep class com.example.BClassBuilder { *; }")

            val outputs = result.output.lines()
            assert("w: [ksp] [workload] Mangled name for internalFun: internalFun\$workload" in outputs)
            assert("w: [ksp] [workload] Mangled name for internalFun: internalFun\$workload" in outputs)
        }
    }
}
