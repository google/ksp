/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ksp.isOpen
import org.jetbrains.kotlin.ksp.isVisibleFrom
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parents

class KSPropertyDeclarationDescriptorImpl(val descriptor: VariableDescriptorWithAccessors) : KSPropertyDeclaration {
    companion object {
        private val cache = mutableMapOf<VariableDescriptorWithAccessors, KSPropertyDeclarationDescriptorImpl>()

        fun getCached(descriptor: VariableDescriptorWithAccessors) = cache.getOrPut(descriptor) { KSPropertyDeclarationDescriptorImpl(descriptor) }
    }

    override val origin = Origin.CLASS

    override val annotations: List<KSAnnotation> by lazy {
        descriptor.annotations.map { KSAnnotationDescriptorImpl.getCached(it) }
    }

    override val containingFile: KSFile? = null

    override val extensionReceiver: KSTypeReference? by lazy {
        if (descriptor.extensionReceiverParameter != null) {
            KSTypeReferenceDescriptorImpl.getCached(descriptor.extensionReceiverParameter!!.type)
        } else {
            null
        }
    }

    override val modifiers: Set<Modifier> by lazy {
        if (descriptor is PropertyDescriptor) {
            val modifiers = mutableSetOf<Modifier>()
            modifiers.addAll(descriptor.toKSModifiers())
            modifiers
        } else {
            emptySet<Modifier>()
        }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        val containingDescriptor = descriptor.parents.first()
        when (containingDescriptor) {
            is ClassDescriptor -> KSClassDeclarationDescriptorImpl.getCached(
                containingDescriptor
            )
            is FunctionDescriptor -> KSFunctionDeclarationDescriptorImpl.getCached(
                containingDescriptor
            )
            else -> null
        } as KSDeclaration?
    }

    override val qualifiedName: KSName by lazy {
        KSNameImpl.getCached(descriptor.fqNameSafe.asString())
    }

    override val setter: KSPropertySetter? by lazy {
        if (descriptor.setter != null) {
            KSPropertySetterDescriptorImpl.getCached(descriptor.setter as PropertySetterDescriptor)
        } else {
            null
        }
    }

    override val getter: KSPropertyGetter? by lazy {
        if (descriptor.getter != null) {
            KSPropertyGetterDescriptorImpl.getCached(descriptor.getter as PropertyGetterDescriptor)
        } else {
            null
        }
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(descriptor.name.asString())
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        descriptor.typeParameters.map { KSTypeParameterDescriptorImpl.getCached(it) }
    }

    override val type: KSTypeReference? by lazy {
        KSTypeReferenceDescriptorImpl.getCached(descriptor.type)
    }

    override fun overrides(overridee: KSPropertyDeclaration): Boolean {
        if (!overridee.isOpen())
            return false
        if (!overridee.isVisibleFrom(this))
            return false
        if (overridee.origin == Origin.JAVA)
            return false
        val superDescriptor = ResolverImpl.instance.resolvePropertyDeclaration(overridee) ?: return false
        return OverridingUtil.DEFAULT.isOverridableBy(
                superDescriptor, descriptor, null
        ).result == OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE
    }

    override fun isDelegated(): Boolean {
        return (descriptor as? PropertyDescriptor)?.delegateField != null
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyDeclaration(this, data)
    }
}