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
package com.google.devtools.ksp.processing

import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * [SymbolProcessor] is the interface used by plugins to integrate into Kotlin Symbol Processing.
 * SymbolProcessor supports multiple round execution, a processor may return a list of deferred symbols at the end
 * of every round, which will be passed to processors again in the next round, together with the newly generated symbols.
 * Upon Exceptions, KSP will try to distinguish the exceptions from KSP and exceptions from processors.
 * Exceptions will result in a termination of processing immediately and be logged as an error in KSPLogger.
 * Exceptions from KSP should be reported to KSP developers for further investigation.
 * At the end of the round where exceptions or errors happened, all processors will invoke onError() function to do
 * their own error handling.
 */
interface SymbolProcessor {
    /**
     * Called by Kotlin Symbol Processing to run the processing task.
     *
     * @param resolver provides [SymbolProcessor] with access to compiler details such as Symbols.
     * @return A list of deferred symbols that the processor can't process.
     */
    fun process(resolver: Resolver): List<KSAnnotated>

    /**
     * Called by Kotlin Symbol Processing to finalize the processing of a compilation.
     */
    fun finish() {}

    /**
     * Called by Kotlin Symbol Processing to handle errors after a round of processing.
     */
    fun onError() {}
}
