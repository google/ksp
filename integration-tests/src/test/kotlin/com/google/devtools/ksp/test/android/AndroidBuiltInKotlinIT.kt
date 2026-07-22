/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.ksp.test.android

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
class AndroidBuiltInKotlinIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "playground-android-builtinkotlin",
        "playground",
        experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

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

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-beta05")

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

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-beta05")
        File(project.root, "gradle.properties").appendText("\nkotlinVersion=2.3.0-Beta2")
        File(project.root, "gradle.properties").appendText("\nksp.project.isolation.enabled=true")

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

    @Test
    fun testRClassResolutionWithBuiltInKotlin() {
        val resDir = File(project.root, "workload/src/main/res/values")
        resDir.mkdirs()
        File(resDir, "strings.xml").writeText(
            """
            <resources>
                <string name="sample_label">Sample Label</string>
            </resources>
            """.trimIndent()
        )

        val javaDir = File(project.root, "workload/src/main/java/com/example")
        javaDir.mkdirs()
        File(javaDir, "RUsage.kt").writeText(
            """
            package com.example
            import com.example.annotation.Builder
            import com.example.myapplication.R

            @Builder
            class RUsage {
                val label = R.string.sample_label
            }
            """.trimIndent()
        )

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "clean", "build", "--configuration-cache", "--info", "--stacktrace"
        ).build().let { result ->
            val classesJar = File(
                project.root,
                "workload/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar"
            )
            JarFile(classesJar).use { jarFile ->
                jarFile.assertContainsNonNullEntry("com/example/RUsageBuilder.class")
            }
        }
    }

    @Test
    fun testRClassResolutionWithBuiltInKotlinAndKapt() {
        val settingsGradle = File(project.root, "settings.gradle.kts")
        val settingsContent = settingsGradle.readText()
        val updatedSettingsContent = settingsContent.replace(
            "id(\"com.android.application\") version agpVersion apply false",
            "id(\"com.android.application\") version agpVersion apply false\n        " +
                "id(\"com.android.legacy-kapt\") version agpVersion apply false"
        )
        settingsGradle.writeText(updatedSettingsContent)

        val buildGradle = File(project.root, "workload/build.gradle.kts")
        val content = buildGradle.readText()
        val updatedContent = content.replace(
            "id(\"com.android.application\")",
            "id(\"com.android.application\")\n    id(\"com.android.legacy-kapt\")"
        )
        buildGradle.writeText(updatedContent)

        val resDir = File(project.root, "workload/src/main/res/values")
        resDir.mkdirs()
        File(resDir, "strings.xml").writeText(
            """
            <resources>
                <string name="sample_label">Sample Label</string>
            </resources>
            """.trimIndent()
        )

        val javaDir = File(project.root, "workload/src/main/java/com/example")
        javaDir.mkdirs()
        File(javaDir, "RUsage.kt").writeText(
            """
            package com.example
            import com.example.annotation.Builder
            import com.example.myapplication.R

            @Builder
            class RUsage {
                val label = R.string.sample_label
            }
            """.trimIndent()
        )

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)

        gradleRunner.withArguments(
            "clean", "build", "--configuration-cache", "--info", "--stacktrace"
        ).build().let { result ->
            val classesJar = File(
                project.root,
                "workload/build/intermediates/compile_app_classes_jar/debug/bundleDebugClassesToCompileJar/classes.jar"
            )
            JarFile(classesJar).use { jarFile ->
                jarFile.assertContainsNonNullEntry("com/example/RUsageBuilder.class")
            }
        }
    }

    @Test
    fun `test Kotlin generated sources are picked up by KSP with built-in Kotlin`() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).forwardOutput()

        File(project.root, "workload/build.gradle.kts").appendText(
            """
                android {
                    androidComponents.onVariants { variant ->
                        val name = variant.name
                
                        val task = project.tasks.register("generate${'$'}{name.replaceFirstChar(Char::uppercase)}KotlinSource", KotlinSourceGenerationTask::class) {
                            getOutputDirectory().set(project.layout.buildDirectory.dir("generated/custom_kotlin/${'$'}{name}"))
                        }
                
                        variant.sources.kotlin?.addGeneratedSourceDirectory(task, KotlinSourceGenerationTask::getOutputDirectory)
                    }
                }

                abstract class KotlinSourceGenerationTask : DefaultTask() {
                    @OutputDirectory
                    abstract fun getOutputDirectory(): DirectoryProperty
                    @TaskAction
                    fun generateCode() {
                        val outDir = getOutputDirectory().get().asFile
                        val outFile = File(outDir, "com/example/GeneratedKotlinClass.kt")
                        outFile.parentFile.mkdirs()
                        outFile.writeText(""${'"'}
                            package com.example
                            class GeneratedKotlinClass {
                                fun foo(): String = "Generated"
                            }
                        ""${'"'}.trimIndent())
                    }
                }
            """.trimIndent()
        )

        File(project.root, "gradle.properties").appendText("\nagpVersion=9.0.0-beta05")

        gradleRunner.withArguments(":workload:assembleDebug", "--stacktrace").build().let { result ->
            Assert.assertEquals(
                TaskOutcome.SUCCESS, result.task(":workload:kspDebugKotlin")?.outcome
            )

            val logFile = File(
                project.root, "workload/build/generated/ksp/debug/resources/TestProcessor.log"
            )
            Assert.assertTrue(
                "Log file expected to exist but it does not exist: ${logFile.path}", logFile.exists()
            )
            val logContent = logFile.readText()
            Assert.assertTrue(logContent.contains("Found GeneratedKotlinClass: com.example.GeneratedKotlinClass"))
        }
    }
}
