/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiParameter
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl

class KSVariableParameterJavaImpl(val psi: PsiParameter) : KSVariableParameter {
    companion object {
        private val cache = mutableMapOf<PsiParameter, KSVariableParameterJavaImpl>()

        fun getCached(psi: PsiParameter) = cache.getOrPut(psi) { KSVariableParameterJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val annotations: List<KSAnnotation> by lazy {
        psi.annotations.map { KSAnnotationJavaImpl.getCached(it) }
    }

    override val isCrossInline: Boolean = false

    override val isNoInline: Boolean = false

    override val isVararg: Boolean = psi.isVarArgs

    override val isVal: Boolean = false

    override val isVar: Boolean = false

    override val name: KSName? by lazy {
        if (psi.name != null) {
            KSNameImpl.getCached(psi.name!!)
        } else {
            null
        }
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceJavaImpl.getCached(psi.type)
    }

    override val hasDefault: Boolean = TODO()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitVariableParameter(this, data)
    }
}