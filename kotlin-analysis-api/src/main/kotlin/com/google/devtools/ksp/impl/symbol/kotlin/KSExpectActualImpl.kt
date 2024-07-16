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
package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSExpectActual
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

class KSExpectActualImpl(private val declarationSymbol: KaDeclarationSymbol) : KSExpectActual {
    override val isActual: Boolean
        get() = declarationSymbol.isActual

    override val isExpect: Boolean
        get() = declarationSymbol.isExpect

    // TODO: not possible in new KMP model, returning empty sequence for now.
    override fun findActuals(): Sequence<KSDeclaration> {
        return emptySequence()
    }

    @OptIn(KaExperimentalApi::class)
    override fun findExpects(): Sequence<KSDeclaration> {
        return if (!isActual) {
            emptySequence()
        } else {
            analyze {
                declarationSymbol.getExpectsForActual().mapNotNull { it.toKSDeclaration() }
            }.asSequence()
        }
    }
}
