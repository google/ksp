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

import com.intellij.psi.PsiParameter
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import com.google.devtools.ksp.symbol.impl.toLocation
import com.intellij.psi.PsiJavaFile

class KSValueParameterJavaImpl private constructor(val psi: PsiParameter) : KSValueParameter {

    companion object : KSObjectCache<PsiParameter, KSValueParameterJavaImpl>() {
        fun getCached(psi: PsiParameter) = cache.getOrPut(psi) { KSValueParameterJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val location: Location by lazy {
        psi.toLocation()
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        psi.annotations.asSequence().map { KSAnnotationJavaImpl.getCached(it) }
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

    override val containingFile: KSFile? by lazy {
        KSFileJavaImpl.getCached(psi.containingFile as PsiJavaFile)
    }

    override val hasDefault: Boolean = false

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueParameter(this, data)
    }

    override fun toString(): String {
        return name?.asString() ?: "_"
    }
}
