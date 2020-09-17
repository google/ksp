/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.java

import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeArgumentImpl
import com.google.devtools.ksp.symbol.impl.toLocation

class KSTypeArgumentJavaImpl private constructor(val psi: PsiType) : KSTypeArgumentImpl() {
    companion object : KSObjectCache<PsiType, KSTypeArgumentJavaImpl>() {
        fun getCached(psi: PsiType) = cache.getOrPut(psi) { KSTypeArgumentJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val location: Location by lazy {
        (psi as? PsiClassReferenceType)?.reference?.toLocation() ?: NonExistLocation
    }

    override val annotations: List<KSAnnotation> by lazy {
        psi.annotations.map { KSAnnotationJavaImpl.getCached(it) }
    }

    // Could be unbounded, need to model unbdouned type argument.
    override val type: KSTypeReference? by lazy {
        KSTypeReferenceJavaImpl.getCached(psi)
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