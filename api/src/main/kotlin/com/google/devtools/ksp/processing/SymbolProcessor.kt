/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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

/**
 * [SymbolProcessor] is the interface used by plugins to integrate into Kotlin Symbol Processing.
 */
interface SymbolProcessor {
    /**
     * Called by Kotlin Symbol Processing to initialize the processor.
     *
     * @param options passed from command line, Gradle, etc.
     * @param kotlinVersion language version of compilation environment.
     * @param codeGenerator creates managed files.
     */
    fun init(options: Map<String, String>, kotlinVersion: KotlinVersion, codeGenerator: CodeGenerator, logger: KSPLogger)

    /**
     * Called by Kotlin Symbol Processing to run the processing task.
     *
     * @param resolver provides [SymbolProcessor] with access to compiler details such as Symbols.
     */
    fun process(resolver: Resolver)

    /**
     * Called by Kotlin Symbol Processing to finalize the processing of a compilation.
     */
    fun finish()
}