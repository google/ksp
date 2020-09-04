/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.KtFunctionType

class KSCallableReferenceImpl private constructor(val ktFunctionType: KtFunctionType) : KSCallableReference {
    companion object : KSObjectCache<KtFunctionType, KSCallableReferenceImpl>() {
        fun getCached(ktFunctionType: KtFunctionType) = cache.getOrPut(ktFunctionType) { KSCallableReferenceImpl(ktFunctionType) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktFunctionType.toLocation()
    }

    override val typeArguments: List<KSTypeArgument> by lazy {
        ktFunctionType.typeArgumentsAsTypes.map { KSTypeArgumentLiteImpl.getCached(it) }
    }

    override val functionParameters: List<KSVariableParameter> by lazy {
        ktFunctionType.parameters.map { KSVariableParameterImpl.getCached(it) }
    }

    override val receiverType: KSTypeReference? by lazy {
        if (ktFunctionType.receiver != null) {
            KSTypeReferenceImpl.getCached(ktFunctionType.receiverTypeReference!!)
        } else {
            null
        }
    }

    override val returnType: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktFunctionType.returnTypeReference!!)
    }

    override fun toString(): String {
        return "${receiverType?.let { "$it." } ?: ""}(${functionParameters.map { it.type.toString() }.joinToString(", ")}) -> $returnType"
    }
}