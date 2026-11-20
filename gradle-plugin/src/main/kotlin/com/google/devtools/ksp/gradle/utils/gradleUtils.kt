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
package com.google.devtools.ksp.gradle.utils

import org.gradle.util.GradleVersion

fun checkMinimumGradleVersion(gradleVersion: GradleVersion = GradleVersion.current()) {
    if (gradleVersion < MINIMUM_SUPPORTED_GRADLE_VERSION) {
        throw RuntimeException(
            "The minimum supported Gradle version is ${MINIMUM_SUPPORTED_GRADLE_VERSION.version}. " +
                "Please upgrade the Gradle version in your project."
        )
    }
}

/** Defines the minimum supported Gradle version. */
val MINIMUM_SUPPORTED_GRADLE_VERSION: GradleVersion = GradleVersion.version("8.13")
