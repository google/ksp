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
package com.google.devtools.ksp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity

private const val KTFMT_VERSION = "0.63"

/**
 * Configures a "ktfmt" task for the project to check formatting and `ktfmtFormat` task
 * to fix formatting. Also registers backward-compatible aliases `ktlint` and `ktlintFormat`.
 */
fun Project.configureKtfmt() {
    val ktfmtCheck = tasks.register("ktfmt", JavaExec::class.java) { task ->
        task.configureCommonKtfmtParams(this@configureKtfmt, dryRun = true)
        task.description = "Check Kotlin code style using ktfmt."
        task.group = "Verification"
    }

    afterEvaluate {
        project.tasks.named("check").configure { checkTask ->
            checkTask.dependsOn(ktfmtCheck)
        }
    }

    val ktfmtFormat = tasks.register("ktfmtFormat", JavaExec::class.java) { task ->
        task.configureCommonKtfmtParams(this@configureKtfmt, dryRun = false)
        task.description = "Fix Kotlin code style deviations using ktfmt."
        task.group = "formatting"
    }
}

private fun JavaExec.configureCommonKtfmtParams(
    project: Project,
    dryRun: Boolean
) {
    val inputFiles = project.fileTree(project.projectDir).also {
        it.include("**/*.kt")
        it.include("**/*.kts")
        it.exclude("**/testData/**")
        it.exclude("**/build/**")
        it.exclude("dist/**")
        it.exclude("**/.*/**")
        it.exclude("**/resources/**")
    }
    val outputFile = project.layout.buildDirectory.file("reports/ktfmt/ktfmtCheckSuccess.txt")
        .map { it.asFile.toRelativeString(project.layout.projectDirectory.asFile) }
        .get()
    inputs.files(inputFiles).withPropertyName("ktfmtInputFiles").withPathSensitivity(PathSensitivity.RELATIVE)
    classpath = project.getKtfmtConfiguration()
    mainClass.set("com.facebook.ktfmt.cli.Main")

    if (dryRun) {
        outputs.file(outputFile).withPropertyName("ktfmtOutputFile")
        outputs.cacheIf { true }
        doLast {
            val file = project.file(outputFile)
            file.parentFile.mkdirs()
            file.writeText("SUCCESS")
        }
    } else {
        outputs.upToDateWhen { false }
    }

    args = listOf("--kotlinlang-style") +
        (if (dryRun) listOf("--dry-run", "--set-exit-if-changed") else emptyList()) +
        inputFiles.files.map { it.toRelativeString(project.projectDir) }
}

private fun Project.getKtfmtConfiguration(): Configuration {
    return configurations.findByName("ktfmt") ?: configurations.create("ktfmt") {
        val dependency = dependencies.create("com.facebook:ktfmt:$KTFMT_VERSION:with-dependencies")
        it.dependencies.add(dependency)
    }
}
