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

package com.google.devtools.ksp.test.secondary

import com.google.devtools.ksp.test.fixtures.TemporaryTestProject
import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class InvalidModuleNameIT(experimentalPsiResolution: Boolean) {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject(
        "playground",
        experimentalPsiResolution = experimentalPsiResolution
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> = listOf(true, false)
    }

    @Test
    fun testInvalidModuleName() {
        // N.B.: The module name is an invalid path name on Windows due to the `:` character.
        //       However, we only care the KSP logs the sanitized name.
        Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows", ignoreCase = true))
        val buildFile = File(project.root, "workload/build.gradle.kts")
        buildFile.appendText(
            """

            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                compilerOptions {
                    moduleName.set("my:invalid:name")
                }
            }
            """.trimIndent()
        )

        val gradleRunner = GradleRunner.create().withProjectDir(project.root)
        val result = gradleRunner.withArguments("clean", "build").build()

        Assert.assertTrue(
            "Expected output to contain log with sanitized module name",
            result.output.contains($$"[my:invalid:name] Mangled name for internalFun: internalFun$my_invalid_name")
        )
        Assert.assertFalse(
            "Expected output NOT to contain log with UNSANITIZED module name",
            result.output.contains($$"[my:invalid:name] Mangled name for internalFun: internalFun$my:invalid:name")
        )
    }
}
