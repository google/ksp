/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSTypeArgumentImpl

class KSTypeArgumentJavaImpl(val psi: PsiType) : KSTypeArgumentImpl() {
    companion object {
        private val cache = mutableMapOf<PsiType, KSTypeArgumentJavaImpl>()

        fun getCached(psi: PsiType) = cache.getOrPut(psi) { KSTypeArgumentJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val annotations: List<KSAnnotation> by lazy {
        psi.annotations.map { KSAnnotationJavaImpl.getCached(it) }
    }

    // Could be unbounded, need to model unbdouned type argument.
    override val type: KSTypeReference? by lazy {
        if (psi is PsiWildcardType) {
            if (psi.bound != null) {
                KSTypeReferenceJavaImpl.getCached(psi.bound!!)
            } else {
                null
            }
        } else {
            KSTypeReferenceJavaImpl.getCached(psi)
        }
    }

    override val variance: Variance by lazy {
        if (psi is PsiWildcardType) {
            when {
                psi.isExtends -> Variance.COVARIANT
                psi.isSuper -> Variance.CONTRAVARIANT
                else -> Variance.INVARIANT
            }
        } else {
            Variance.INVARIANT
        }
    }
}