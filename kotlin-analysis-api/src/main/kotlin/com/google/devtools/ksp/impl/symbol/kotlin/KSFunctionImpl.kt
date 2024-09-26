/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.util.*

class KSFunctionImpl @OptIn(KaExperimentalApi::class) constructor(
    val functionSignature: KaFunctionSignature<KaFunctionSymbol>,
    private val substitutor: KaSubstitutor,
) : KSFunction {

    override val returnType: KSType? by lazy {
        functionSignature.returnType.let { KSTypeImpl.getCached(it) }
    }

    override val parameterTypes: List<KSType?> by lazy {
        functionSignature.valueParameters.map { it.returnType.let { KSTypeImpl.getCached(it) } }
    }

    @OptIn(KaExperimentalApi::class)
    override val typeParameters: List<KSTypeParameter> by lazy {
        functionSignature.symbol.typeParameters.map { typeParam ->
            val bounds = typeParam.upperBounds.ifNotEmpty {
                map { bound ->
                    // Substitude to a fixed point.
                    var prev = bound
                    var curr = substitutor.substitute(bound)
                    val seen = mutableSetOf<Pair<KaType, KaType>>()
                    while (prev != curr) {
                        val pair = Pair(prev, curr)
                        if (pair in seen) {
                            val msg = "Recurrent substitution of bounds of $typeParam: $bound by $substitutor"
                            throw IllegalStateException(msg)
                        }
                        seen.add(pair)
                        prev = curr
                        curr = substitutor.substitute(curr)
                    }
                    curr
                }
            }
            KSTypeParameterImpl.getCached(typeParam, bounds)
        }
    }

    override val extensionReceiverType: KSType? by lazy {
        functionSignature.receiverType?.let { KSTypeImpl.getCached(it) }
    }

    override val isError: Boolean = false

    private val cachedHashCode by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Objects.hash(
            returnType ?: 0,
            parameterTypes,
            typeParameters,
            extensionReceiverType ?: 0
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KSFunctionImpl

        if (returnType != other.returnType) return false
        if (parameterTypes != other.parameterTypes) return false
        if (typeParameters != other.typeParameters) return false
        if (extensionReceiverType != other.extensionReceiverType) return false

        return true
    }

    override fun hashCode() = cachedHashCode
}
