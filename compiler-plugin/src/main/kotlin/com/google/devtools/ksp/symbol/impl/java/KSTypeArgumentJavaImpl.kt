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
                psi.bound == null -> Variance.STAR
                else -> Variance.INVARIANT
            }
        } else {
            Variance.INVARIANT
        }
    }
}