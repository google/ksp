/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import com.google.devtools.kotlin.symbol.processing.symbol.impl.toKSModifiers
import com.google.devtools.kotlin.symbol.processing.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

class KSPropertySetterImpl private constructor(ktPropertySetter: KtPropertyAccessor) : KSPropertyAccessorImpl(ktPropertySetter),
    KSPropertySetter {
    companion object : KSObjectCache<KtPropertyAccessor, KSPropertySetterImpl>() {
        fun getCached(ktPropertySetter: KtPropertyAccessor) = cache.getOrPut(ktPropertySetter) { KSPropertySetterImpl(ktPropertySetter) }
    }

    override val parameter: KSVariableParameter by lazy {
        ktPropertySetter.parameterList?.parameters?.singleOrNull()?.let { KSVariableParameterImpl.getCached(it) }
            ?: throw IllegalStateException()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertySetter(this, data)
    }

    override fun toString(): String {
        return "$receiver.setter()"
    }
}