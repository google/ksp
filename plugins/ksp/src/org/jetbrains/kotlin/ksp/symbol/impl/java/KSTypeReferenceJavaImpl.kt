/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.*
import org.jetbrains.kotlin.ksp.symbol.*

class KSTypeReferenceJavaImpl(val psi: PsiType) : KSTypeReference {
    companion object {
        private val cache = mutableMapOf<PsiType, KSTypeReferenceJavaImpl>()

        fun getCached(psi: PsiType) = cache.getOrPut(psi) { KSTypeReferenceJavaImpl(psi) }
    }

    override val annotations: List<KSAnnotation> by lazy {
        psi.annotations.map { KSAnnotationJavaImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val element: KSReferenceElement by lazy {
        val type = if (psi is PsiWildcardType) {
            psi.bound
        } else {
            psi
        }
        when (type) {
            is PsiClassType -> KSClassifierReferenceJavaImpl.getCached(type)
            is PsiMethodReferenceType -> KSCallableReferenceJavaImpl.getCached(type)
            is PsiWildcardType -> KSClassifierReferenceJavaImpl.getCached(type.extendsBound as PsiClassType)
            else -> throw IllegalStateException()
        }
    }

    override fun resolve(): KSType? {
        TODO()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }
}