/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.test

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.io.File

class AGP741IT {
    @Rule
    @JvmField
    val project: TemporaryTestProject = TemporaryTestProject("playground-android-multi", "playground")

    @Test
    fun testDependencyResolutionCheck() {
        val gradleRunner = GradleRunner.create().withProjectDir(project.root).withGradleVersion("7.6")

        File(project.root, "gradle.properties").appendText("\nagpVersion=7.4.1")
        gradleRunner.withArguments(":workload:compileDebugKotlin").build().let { result ->
            Assert.assertFalse(result.output.contains("was resolved during configuration time."))
        }
    }
}
