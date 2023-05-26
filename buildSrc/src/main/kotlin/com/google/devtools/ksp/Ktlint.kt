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
package com.google.devtools.ksp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity

// This file is mostly ported from AndroidX with minor modifications.
// https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/src/main/kotlin/androidx/build/Ktlint.kt

/**
 * Adds a ktlintApplyToIdea task that updates the local .idea files to match the ktlint style.
 */
fun Project.configureKtlintApplyToIdea() {
    if (this.rootProject !== this) {
        throw IllegalArgumentException("Can only use root project for applyToIdea task")
    }
    tasks.register("ktlintApplyToIdea", JavaExec::class.java) { task ->
        task.description = "Apply ktlint style to idea"
        task.group = "Tooling"
        task.classpath = getKtlintConfiguration()
        task.mainClass.set("com.pinterest.ktlint.Main")
        task.args = listOf(
            "applyToIDEAProject",
            "-y"
        )
    }
}

/**
 * Configures a "ktlint" task for the project to check formatting and `ktlintFormat` task
 * to fix formatting.
 */
fun Project.configureKtlint() {
    val lintProvider = tasks.register("ktlint", JavaExec::class.java) { task ->
        task.configureCommonKtlintParams(this@configureKtlint)
        task.description = "Check Kotlin code style."
        task.group = "Verification"
    }

    afterEvaluate {
        // check task is not available yet, which is why we use afterEvaluate
        project.tasks.named("check").configure { checkTask ->
            checkTask.dependsOn(lintProvider)
        }
    }

    tasks.register("ktlintFormat", JavaExec::class.java) { task ->
        task.configureCommonKtlintParams(this@configureKtlint)
        task.description = "Fix Kotlin code style deviations."
        task.group = "formatting"
        task.args = listOf("-F") + task.args!!
    }
}

/**
 * Configures common ktlint parameters for ktlint tasks
 */
private fun JavaExec.configureCommonKtlintParams(
    project: Project
) {
    val ktlintInputFiles = project.fileTree(project.projectDir).also {
        it.include("**/*.kt")
        it.include("**/*.kts")
        it.exclude("**/testData/**")
        it.exclude("**/build/**")
        it.exclude("dist/**")
        it.exclude("**/.*/**")
    }
    val outputFile = project.buildDir.resolve("reports/ktlint/ktlint-checkstyle-report.xml").toRelativeString(project.projectDir)
    inputs.files(ktlintInputFiles).withPropertyName("ktlintInputFiles").withPathSensitivity(PathSensitivity.RELATIVE)
    classpath = project.getKtlintConfiguration()
    mainClass.set("com.pinterest.ktlint.Main")
    outputs.file(outputFile).withPropertyName("ktlintOutputFile")
    outputs.cacheIf { true }
    args = listOf(
        "--reporter=plain",
        "--reporter=checkstyle,output=$outputFile",
    ) + ktlintInputFiles.files.map { it.toRelativeString(project.projectDir) }
}

private fun Project.getKtlintConfiguration(): Configuration {
    return configurations.findByName("ktlint") ?: configurations.create("ktlint") {
        val dependency = dependencies.create("com.pinterest:ktlint:0.40.0")
        it.dependencies.add(dependency)
    }
}
