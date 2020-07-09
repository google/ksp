/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ksp.isOpen
import org.jetbrains.kotlin.ksp.isVisibleFrom
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.findParentDeclaration
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtStubbedPsiUtil
import org.jetbrains.kotlin.resolve.OverridingUtil

class KSPropertyDeclarationParameterImpl(val ktParameter: KtParameter) : KSPropertyDeclaration {
    companion object {
        private val cache = mutableMapOf<KtParameter, KSPropertyDeclarationParameterImpl>()

        fun getCached(ktParameter: KtParameter) = cache.getOrPut(ktParameter) { KSPropertyDeclarationParameterImpl(ktParameter) }
    }

    override val origin = Origin.KOTLIN

    override val extensionReceiver: KSTypeReference? = null

    override val getter: KSPropertyGetter? = null

    override val setter: KSPropertySetter? = null

    override val type: KSTypeReference? by lazy {
        if (ktParameter.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktParameter.typeReference!!)
        } else {
            null
        }
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktParameter.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val containingFile: KSFile? by lazy {
        KSFileImpl.getCached(ktParameter.containingKtFile)
    }

    override val modifiers: Set<Modifier> by lazy {
        ktParameter.toKSModifiers()
    }

    override val parentDeclaration: KSDeclaration by lazy {
        KtStubbedPsiUtil.getContainingDeclaration(ktParameter)!!.findParentDeclaration()!!
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached("${parentDeclaration.qualifiedName!!.asString()}.${simpleName.asString()}")
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktParameter.name ?: "_")
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        ktParameter.typeParameters.map {
            KSTypeParameterImpl.getCached(it, KtStubbedPsiUtil.getContainingDeclaration(ktParameter, KtClassOrObject::class.java)!!)
        }
    }

    override fun isDelegated(): Boolean = false

    override fun overrides(overridee: KSPropertyDeclaration): Boolean {
        if (!this.modifiers.contains(Modifier.OVERRIDE))
            return false
        if (!overridee.isOpen())
            return false
        if (!overridee.isVisibleFrom(this))
            return false
        if (overridee.origin == Origin.JAVA)
            return false
        val superDescriptor = ResolverImpl.instance.resolvePropertyDeclaration(overridee) ?: return false
        val subDescriptor = ResolverImpl.instance.resolveDeclaration(ktParameter) as PropertyDescriptor
        return OverridingUtil.DEFAULT.isOverridableBy(
                superDescriptor, subDescriptor, null
        ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }
}
