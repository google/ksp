/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processing

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