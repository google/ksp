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
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters

class KSFunctionImpl(val ktFunctionLikeSymbol: KaFunctionSignature<KaFunctionSymbol>) : KSFunction {

    override val returnType: KSType? by lazy {
        ktFunctionLikeSymbol.returnType.let { KSTypeImpl.getCached(it) }
    }

    override val parameterTypes: List<KSType?> by lazy {
        ktFunctionLikeSymbol.valueParameters.map { it.returnType.let { KSTypeImpl.getCached(it) } }
    }

    @OptIn(KaExperimentalApi::class)
    override val typeParameters: List<KSTypeParameter> by lazy {
        (ktFunctionLikeSymbol as? KaDeclarationSymbol)?.typeParameters?.map { KSTypeParameterImpl.getCached(it) }
            ?: emptyList()
    }

    override val extensionReceiverType: KSType? by lazy {
        ktFunctionLikeSymbol.receiverType?.let { KSTypeImpl.getCached(it) }
    }

    override val isError: Boolean = false
}
