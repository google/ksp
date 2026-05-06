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

package com.google.devtools.ksp.test.annotations

/**
 * Indicates that a test case was introduced to reproduce or verify a fix for a specific bug.
 *
 * This annotation helps track the origin of tests back to their corresponding issue reports
 * (e.g., GitHub issues, YouTrack tickets).
 *
 * @property id The unique identifier of the issue (e.g., a GitHub issue link or `KT-` identifier).
 * @property description An optional brief explanation of the bug or why the test was added.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Repeatable
annotation class Bug(val id: String, val description: String = "")
