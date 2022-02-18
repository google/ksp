/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.processing

class SymbolProcessorEnvironment(
    /**
     * passed from command line, Gradle, etc.
     */
    val options: Map<String, String>,

    /**
     * language version of compilation environment.
     */
    val kotlinVersion: KotlinVersion,

    /**
     * creates managed files.
     */
    val codeGenerator: CodeGenerator,

    /**
     * for logging to build output.
     */
    val logger: KSPLogger,

    /**
     * Kotlin API version of compilation environment.
     */
    val apiVersion: KotlinVersion,

    /**
     * Kotlin compiler version of compilation environment.
     */
    val compilerVersion: KotlinVersion,

    /**
     * Information of target platforms
     *
     * There can be multiple platforms in a metadata compilation.
     */
    val platforms: List<PlatformInfo>,
) {
    // For compatibility with KSP 1.0.2 and earlier
    constructor(
        options: Map<String, String>,
        kotlinVersion: KotlinVersion,
        codeGenerator: CodeGenerator,
        logger: KSPLogger
    ) : this(
        options,
        kotlinVersion,
        codeGenerator,
        logger,
        kotlinVersion,
        kotlinVersion,
        emptyList()
    )
}
