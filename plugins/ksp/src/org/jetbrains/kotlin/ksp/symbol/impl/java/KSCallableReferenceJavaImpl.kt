/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiMethodReferenceType
import org.jetbrains.kotlin.ksp.symbol.*

// Possibly wrong implementation, method reference is not function type
class KSCallableReferenceJavaImpl(val psi: PsiMethodReferenceType) : KSCallableReference {
    companion object {
        private val cache = mutableMapOf<PsiMethodReferenceType, KSCallableReferenceJavaImpl>()

        fun getCached(psi: PsiMethodReferenceType) = cache.getOrPut(psi) { KSCallableReferenceJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val functionParameters: List<KSVariableParameter>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val receiverType: KSTypeReference? = null

    override val typeArguments: List<KSTypeArgument>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val returnType: KSTypeReference
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}