/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.psi.KtPropertyAccessor

class KSPropertySetterImpl private constructor(val ktPropertySetter: KtPropertyAccessor) : KSPropertySetter {
    companion object : KSObjectCache<KtPropertyAccessor, KSPropertySetterImpl>() {
        fun getCached(ktPropertySetter: KtPropertyAccessor) = cache.getOrPut(ktPropertySetter) { KSPropertySetterImpl(ktPropertySetter) }
    }

    override val origin = Origin.KOTLIN

    override val parameter: KSVariableParameter by lazy {
        ktPropertySetter.parameterList?.parameters?.singleOrNull()?.let { KSVariableParameterImpl.getCached(it) } ?: throw IllegalStateException()
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktPropertySetter.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        ktPropertySetter.toKSModifiers()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertySetter(this, data)
    }
}