/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiClassType
import org.jetbrains.kotlin.ksp.symbol.KSTypeArgument
import org.jetbrains.kotlin.ksp.symbol.KSClassifierReference
import org.jetbrains.kotlin.ksp.symbol.Origin

class KSClassifierReferenceJavaImpl(val psi: PsiClassType) : KSClassifierReference {
    companion object {
        private val cache = mutableMapOf<PsiClassType, KSClassifierReferenceJavaImpl>()

        fun getCached(psi: PsiClassType) = cache.getOrPut(psi) { KSClassifierReferenceJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val qualifier: KSClassifierReference?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    // PsiClassType.parameters is semantically argument
    override val typeArguments: List<KSTypeArgument> by lazy {
        psi.parameters.map { KSTypeArgumentJavaImpl.getCached(it) }
    }

    override fun referencedName(): String {
        return psi.className
    }
}