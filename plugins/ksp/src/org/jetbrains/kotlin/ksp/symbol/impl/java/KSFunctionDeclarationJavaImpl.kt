/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.lang.jvm.JvmModifier
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.findParentDeclaration
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers

class KSFunctionDeclarationJavaImpl(val psi: PsiMethod) : KSFunctionDeclaration {
    companion object {
        private val cache = mutableMapOf<PsiMethod, KSFunctionDeclarationJavaImpl>()

        fun getCached(psi: PsiMethod) = cache.getOrPut(psi) { KSFunctionDeclarationJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val annotations: List<KSAnnotation> by lazy {
        psi.annotations.map { KSAnnotationJavaImpl.getCached(it) }
    }

    override val containingFile: KSFile? by lazy {
        KSFileJavaImpl.getCached(psi.containingFile as PsiJavaFile)
    }

    override fun overrides(overridee: KSFunctionDeclaration): Boolean {
        TODO("Not yet implemented")
    }

    //TODO: whether we want to model it
    override val declarations: List<KSDeclaration> = emptyList()

    override val extensionReceiver: KSTypeReference? = null


    override val functionKind: FunctionKind = if (psi.hasModifier(JvmModifier.STATIC)) FunctionKind.STATIC else FunctionKind.MEMBER

    override val modifiers: Set<Modifier> by lazy {
        psi.toKSModifiers()
    }

    override val parameters: List<KSVariableParameter> by lazy {
        psi.parameterList.parameters.map { KSVariableParameterJavaImpl.getCached(it) }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        psi.findParentDeclaration()
    }

    override val qualifiedName: KSName
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val returnType: KSTypeReference? by lazy {
        if (psi.returnType != null) {
            KSTypeReferenceJavaImpl.getCached(psi.returnType!!)
        } else {
            null
        }
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(psi.name)
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        psi.typeParameters.map { KSTypeParameterJavaImpl.getCached(it) }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFunctionDeclaration(this, data)
    }
}