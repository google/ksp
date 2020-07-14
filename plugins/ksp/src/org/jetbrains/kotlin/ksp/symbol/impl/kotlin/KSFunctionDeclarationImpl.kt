/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ksp.isOpen
import org.jetbrains.kotlin.ksp.isVisibleFrom
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.*
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSFunctionDeclarationDescriptorImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.calls.inference.returnTypeOrNothing
import java.lang.IllegalStateException

class KSFunctionDeclarationImpl private constructor(val ktFunction: KtFunction) : KSFunctionDeclaration {
    companion object : KSObjectCache<KtFunction, KSFunctionDeclarationImpl>() {
        fun getCached(ktFunction: KtFunction) = cache.getOrPut(ktFunction) { KSFunctionDeclarationImpl(ktFunction) }
    }

    override val origin = Origin.KOTLIN

    override val annotations: List<KSAnnotation> by lazy {
        ktFunction.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val containingFile: KSFile by lazy {
        KSFileImpl.getCached(ktFunction.containingKtFile)
    }

    override fun overrides(overridee: KSFunctionDeclaration): Boolean {
        if (!this.modifiers.contains(Modifier.OVERRIDE))
            return false
        if (!overridee.isOpen())
            return false
        if (!overridee.isVisibleFrom(this))
            return false
        val superDescriptor = ResolverImpl.instance.resolveFunctionDeclaration(overridee) ?: return false
        val subDescriptor = ResolverImpl.instance.resolveDeclaration(ktFunction) as FunctionDescriptor
        return OverridingUtil.DEFAULT.isOverridableBy(
            superDescriptor, subDescriptor, null
        ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
    }

    override val declarations: List<KSDeclaration> by lazy {
        if (!ktFunction.hasBlockBody()) {
            emptyList()
        } else {
            ktFunction.bodyBlockExpression?.statements?.getKSDeclarations() ?: emptyList()
        }
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        if (ktFunction.receiverTypeReference != null) {
            KSTypeReferenceImpl.getCached(ktFunction.receiverTypeReference!!)
        } else {
            null
        }
    }

    override val functionKind: FunctionKind by lazy {
        if (parentDeclaration == null) {
            FunctionKind.STATIC
        } else {
            when (ktFunction) {
                is KtNamedFunction, is KtPrimaryConstructor, is KtSecondaryConstructor -> FunctionKind.MEMBER
                is KtFunctionLiteral -> if (ktFunction.node.findChildByType(KtTokens.FUN_KEYWORD) != null) FunctionKind.ANONYMOUS else FunctionKind.LAMBDA
                else -> throw IllegalStateException()
            }
        }
    }

    override val isAbstract: Boolean by lazy {
        this.modifiers.contains(Modifier.ABSTRACT) ||
                ((this.parentDeclaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE
                        && !this.ktFunction.hasBody())
    }

    override val modifiers: Set<Modifier> by lazy {
        ktFunction.toKSModifiers()
    }

    override val parameters: List<KSVariableParameter> by lazy {
        ktFunction.valueParameters.map { KSVariableParameterImpl.getCached(it) }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ktFunction.findParentDeclaration()
    }

    override val qualifiedName: KSName? by lazy {
        ktFunction.fqName?.asString()?.let { KSNameImpl.getCached(it) }
    }

    override val returnType: KSTypeReference by lazy {
        if (ktFunction.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktFunction.typeReference!!)
        } else {
            KSTypeReferenceDeferredImpl.getCached {
                val desc = ResolverImpl.instance.resolveDeclaration(ktFunction) as FunctionDescriptor
                KSTypeImpl.getCached(desc.returnTypeOrNothing)
            }
        }
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktFunction.name!!)
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktFunction.typeParameters.map { KSTypeParameterImpl.getCached(it, ktFunction) }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFunctionDeclaration(this, data)
    }
}

