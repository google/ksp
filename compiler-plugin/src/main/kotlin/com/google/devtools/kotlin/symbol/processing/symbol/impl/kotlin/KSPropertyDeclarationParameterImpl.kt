/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import com.google.devtools.kotlin.symbol.processing.isOpen
import com.google.devtools.kotlin.symbol.processing.isPrivate
import com.google.devtools.kotlin.symbol.processing.isVisibleFrom
import com.google.devtools.kotlin.symbol.processing.processing.impl.ResolverImpl
import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.binary.KSPropertyGetterDescriptorImpl
import com.google.devtools.kotlin.symbol.processing.symbol.impl.binary.KSPropertySetterDescriptorImpl
import com.google.devtools.kotlin.symbol.processing.symbol.impl.synthetic.KSPropertyGetterSyntheticImpl
import com.google.devtools.kotlin.symbol.processing.symbol.impl.synthetic.KSPropertySetterSyntheticImpl
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtStubbedPsiUtil
import org.jetbrains.kotlin.resolve.OverridingUtil

class KSPropertyDeclarationParameterImpl private constructor(val ktParameter: KtParameter) : KSPropertyDeclaration,
    KSDeclarationImpl(ktParameter),
    KSExpectActual by KSExpectActualImpl(ktParameter) {
    companion object : KSObjectCache<KtParameter, KSPropertyDeclarationParameterImpl>() {
        fun getCached(ktParameter: KtParameter) = cache.getOrPut(ktParameter) { KSPropertyDeclarationParameterImpl(ktParameter) }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ktParameter.findParentDeclaration()!!.parentDeclaration
    }

    override val extensionReceiver: KSTypeReference? = null

    override val isMutable: Boolean by lazy {
        ktParameter.isMutable
    }

    override val getter: KSPropertyGetter? by lazy {
        if (this.isPrivate()) {
            null
        } else {
            KSPropertyGetterSyntheticImpl.getCached(this)
        }
    }

    override val setter: KSPropertySetter? by lazy {
        if (ktParameter.isMutable && !this.isPrivate()) {
            KSPropertySetterSyntheticImpl.getCached(this)
        } else {
            null
        }
    }

    override val type: KSTypeReference? by lazy {
        if (ktParameter.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktParameter.typeReference!!)
        } else {
            null
        }
    }

    override fun isDelegated(): Boolean = false

    override fun findOverridee(): KSPropertyDeclaration? {
        return ResolverImpl.instance.resolvePropertyDeclaration(this)?.original?.overriddenDescriptors?.single { it.overriddenDescriptors.isEmpty() }
            ?.toKSPropertyDeclaration()
    }

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
