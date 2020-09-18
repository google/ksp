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

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toLocation

class KSClassifierReferenceJavaImpl private constructor(val psi: PsiClassType) : KSClassifierReference {
    companion object : KSObjectCache<PsiClassType, KSClassifierReferenceJavaImpl>() {
        fun getCached(psi: PsiClassType) = cache.getOrPut(psi) { KSClassifierReferenceJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val location: Location by lazy {
        (psi as? PsiJavaCodeReferenceElement)?.toLocation() ?: NonExistLocation
    }

    override val qualifier: KSClassifierReference? by lazy {
        val qualifierReference = (psi as? PsiClassReferenceType)?.reference?.qualifier as? PsiJavaCodeReferenceElement ?: return@lazy null
        val qualifierType = PsiClassReferenceType(qualifierReference, psi.languageLevel)
        getCached(qualifierType)
    }

    // PsiClassType.parameters is semantically argument
    override val typeArguments: List<KSTypeArgument> by lazy {
        psi.parameters.map { KSTypeArgumentJavaImpl.getCached(it) }
    }

    override fun referencedName(): String {
        return psi.className + if (psi.parameterCount > 0) "<${psi.parameters.map { it.presentableText }.joinToString(", ")}>" else ""
    }

    override fun toString() = referencedName()
}
