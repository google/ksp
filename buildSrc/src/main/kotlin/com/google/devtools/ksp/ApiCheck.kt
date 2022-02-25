/*
 * Copyright 2022 Google LLC
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
package com.google.devtools.ksp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec

val API_BASE_FILE="api.base"

/**
 * Adapted from ktlint
 */
fun Project.configureMetalava() {
    val checkProvider = tasks.register("checkApi", JavaExec::class.java) { task ->
        task.configureCommonMetalavaArgs(this@configureMetalava)
        task.description = "Check API compatibility."
        task.group = "Verification"
        task.args = listOf("--check-compatibility:api:released", API_BASE_FILE) + task.args!!
    }

    afterEvaluate {
        // check task is not available yet, which is why we use afterEvaluate
        project.tasks.named("check").configure { checkTask ->
            checkTask.dependsOn(checkProvider)
        }
    }

    tasks.register("updateApi", JavaExec::class.java) { task ->
        task.configureCommonMetalavaArgs(this@configureMetalava)
        task.description = "Update API base file."
        task.group = "formatting"
        task.args = listOf("--api", API_BASE_FILE) + task.args!!
    }
}

/**
 * Configures common Metalava parameters
 */
private fun JavaExec.configureCommonMetalavaArgs(
    project: Project
) {
    val jdkHome = org.gradle.internal.jvm.Jvm.current().getJavaHome().absolutePath
    val compileClasspath = project.getCompileClasspath()
    val apiFiles = project.fileTree(project.projectDir).also {
        it.include("**/*.kt")
        it.include("**/*.java")
        it.exclude("**/testData/**")
        it.exclude("**/build/**")
        it.exclude("**/.*/**")
    }
    inputs.files(apiFiles)
    classpath = project.getMetalavaConfiguration()
    main = "com.android.tools.metalava.Driver"
    args = listOf(
        "--jdk-home", jdkHome,
        "--classpath", compileClasspath,
        "--source-files",
    ) + apiFiles.files.map { it.absolutePath }
}

private fun Project.getCompileClasspath(): String =
    configurations.findByName("compileClasspath")!!.files.map { it.absolutePath }.joinToString(":")

private fun Project.getMetalavaConfiguration(): Configuration {
    return configurations.findByName("metalava") ?: configurations.create("metalava") {
        val dependency = dependencies.create("com.android.tools.metalava:metalava:1.0.0-alpha04")
        it.dependencies.add(dependency)
    }
}
