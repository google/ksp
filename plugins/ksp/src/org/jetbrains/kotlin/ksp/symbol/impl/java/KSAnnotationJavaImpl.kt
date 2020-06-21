/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl

class KSAnnotationJavaImpl(val psi: PsiAnnotation) : KSAnnotation {
    companion object {
        private val cache = mutableMapOf<PsiAnnotation, KSAnnotationJavaImpl>()

        fun getCached(psi: PsiAnnotation) = cache.getOrPut(psi) { KSAnnotationJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val annotationType: KSTypeReference by lazy {
        KSTypeReferenceLiteJavaImpl.getCached(KSClassDeclarationJavaImpl.getCached(psi.nameReferenceElement!!.resolve() as PsiClass).asType(emptyList()))
    }

    override val arguments: List<KSValueArgument> by lazy {
        psi.parameterList.attributes
            .map {
                if (it.name != null) KSValueArgumentJavaImpl.getCached(KSNameImpl.getCached(it.name!!), it.attributeValue)
                else KSValueArgumentJavaImpl.getCached(null, it.attributeValue)
            }
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(psi.qualifiedName!!.split(".").last())
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }
}