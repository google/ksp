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

package com.google.devtools.ksp.gradle

import org.gradle.api.logging.Logger
import org.gradle.api.model.ObjectFactory
import org.gradle.util.GradleVersion

internal object ProblemsKspDiagnosticReporterFactory {
    fun isSupported(gradleVersion: GradleVersion, classLoader: ClassLoader): Boolean {
        if (gradleVersion.baseVersion < GradleVersion.version("8.13")) return false
        return try {
            listOf("Problems", "ProblemReporter", "ProblemSpec", "ProblemId", "ProblemGroup", "Severity")
                .forEach { Class.forName("org.gradle.api.problems.$it", false, classLoader) }
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: LinkageError) {
            false
        }
    }

    fun usesProblemsConsole(
        gradleVersion: GradleVersion,
        warningModeAll: Boolean,
        warnEnabled: Boolean,
    ): Boolean =
        warningModeAll && warnEnabled && gradleVersion.baseVersion >= GradleVersion.version("9.3")

    fun create(
        delegate: KspDiagnosticReporter,
        objects: ObjectFactory,
        logger: Logger,
        warningModeAll: Boolean,
        taskPath: String,
        gradleVersion: GradleVersion = GradleVersion.current(),
        classLoader: ClassLoader = javaClass.classLoader,
    ): KspDiagnosticReporter {
        if (!isSupported(gradleVersion, classLoader)) return delegate
        // Load optional Problems types only after checking the Gradle capabilities.
        val implementation = classLoader.loadClass("com.google.devtools.ksp.gradle.ProblemsKspDiagnosticReporter")
            .asSubclass(KspDiagnosticReporter::class.java)
        return objects.newInstance(
            implementation,
            delegate,
            usesProblemsConsole(gradleVersion, warningModeAll, logger.isWarnEnabled),
            taskPath,
        )
    }
}
