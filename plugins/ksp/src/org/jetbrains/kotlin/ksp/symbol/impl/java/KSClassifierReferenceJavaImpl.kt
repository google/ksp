/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.kotlin.ksp.symbol.KSClassifierReference
import org.jetbrains.kotlin.ksp.symbol.KSTypeArgument
import org.jetbrains.kotlin.ksp.symbol.Origin

class KSClassifierReferenceJavaImpl(val psi: PsiClassType) : KSClassifierReference {
    companion object {
        private val cache = mutableMapOf<PsiClassType, KSClassifierReferenceJavaImpl>()

        fun getCached(psi: PsiClassType) = cache.getOrPut(psi) { KSClassifierReferenceJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

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
