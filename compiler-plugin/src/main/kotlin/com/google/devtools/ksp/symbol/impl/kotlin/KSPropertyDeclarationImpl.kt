/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptorWithAccessors
import com.google.devtools.ksp.isLocal
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.isPrivate
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.*
import com.google.devtools.ksp.symbol.impl.synthetic.KSPropertyGetterSyntheticImpl
import com.google.devtools.ksp.symbol.impl.synthetic.KSPropertySetterSyntheticImpl
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import org.jetbrains.kotlin.resolve.OverridingUtil

class KSPropertyDeclarationImpl private constructor(val ktProperty: KtProperty) : KSPropertyDeclaration, KSDeclarationImpl(ktProperty),
    KSExpectActual by KSExpectActualImpl(ktProperty) {
    companion object : KSObjectCache<KtProperty, KSPropertyDeclarationImpl>() {
        fun getCached(ktProperty: KtProperty) = cache.getOrPut(ktProperty) { KSPropertyDeclarationImpl(ktProperty) }
    }

    override val extensionReceiver: KSTypeReference? by lazy {
        if (ktProperty.isExtensionDeclaration()) {
            KSTypeReferenceImpl.getCached(ktProperty.receiverTypeReference!!)
        } else {
            null
        }
    }

    override val isMutable: Boolean by lazy {
        ktProperty.isVar
    }

    private fun shouldCreateSyntheticAccessor(): Boolean {
        return !this.isPrivate()
                && (
                !this.isLocal() && !this.modifiers.contains(Modifier.ABSTRACT)
                        && (this.parentDeclaration as? KSClassDeclaration)?.classKind != ClassKind.INTERFACE
                        || ((this.parentDeclaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE && ktProperty.accessors.isNotEmpty())
                )
    }

    override val getter: KSPropertyGetter? by lazy {
        if (!shouldCreateSyntheticAccessor()) {
            null
        } else {
            val getter = ktProperty.accessors.filter { it.isGetter }.singleOrNull()
            if (getter != null) {
                KSPropertyGetterImpl.getCached(getter)
            } else {
                KSPropertyGetterSyntheticImpl.getCached(this)
            }
        }
    }

    override val setter: KSPropertySetter? by lazy {
        if (!shouldCreateSyntheticAccessor() || !ktProperty.isVar) {
            null
        } else {
            val setter = ktProperty.accessors.filter { it.isSetter }.singleOrNull()
            if (setter != null) {
                KSPropertySetterImpl.getCached(setter)
            } else {
                KSPropertySetterSyntheticImpl.getCached(this)
            }
        }
    }

    override val type: KSTypeReference? by lazy {
        if (ktProperty.typeReference != null) {
            KSTypeReferenceImpl.getCached(ktProperty.typeReference!!)
        } else {
            KSTypeReferenceDeferredImpl.getCached {
                val desc = ResolverImpl.instance.resolveDeclaration(ktProperty) as? VariableDescriptorWithAccessors
                if (desc == null) {
                    // TODO: Add error type to allow forward reference being taken care of.
                    null
                } else {
                    getKSTypeCached(desc.type)
                }
            }
        }
    }

    override fun isDelegated(): Boolean = ktProperty.hasDelegate()

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
        val subDescriptor = ResolverImpl.instance.resolveDeclaration(ktProperty) as PropertyDescriptor
        return OverridingUtil.DEFAULT.isOverridableBy(
                superDescriptor, subDescriptor, null
        ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
    }

    override fun findOverridee(): KSPropertyDeclaration? {
        return ResolverImpl.instance.resolvePropertyDeclaration(this)?.original?.overriddenDescriptors?.single { it.overriddenDescriptors.isEmpty() }
            ?.toKSPropertyDeclaration()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }
}