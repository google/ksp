/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.ksp.symbol.impl.toKSVariance
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KSTypeParameterDescriptorImpl private constructor(val descriptor: TypeParameterDescriptor) : KSTypeParameter {
    companion object : KSObjectCache<TypeParameterDescriptor, KSTypeParameterDescriptorImpl>() {
        fun getCached(descriptor: TypeParameterDescriptor) = cache.getOrPut(descriptor) { KSTypeParameterDescriptorImpl(descriptor) }
    }

    override val origin = Origin.CLASS

    override val location: Location = NonExistLocation

    override val bounds: List<KSTypeReference> by lazy {
        descriptor.upperBounds.map { KSTypeReferenceDescriptorImpl.getCached(it) }
    }
    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(descriptor.name.asString())
    }

    override val qualifiedName: KSName? by lazy {
        KSNameImpl.getCached(descriptor.fqNameSafe.asString())
    }

    override val typeParameters: List<KSTypeParameter> = emptyList()

    override val parentDeclaration: KSDeclaration? by lazy {
        when (val parent = descriptor.containingDeclaration) {
            is ClassDescriptor -> KSClassDeclarationDescriptorImpl.getCached(parent)
            is FunctionDescriptor -> KSFunctionDeclarationDescriptorImpl.getCached(parent)
            is PropertyDescriptor -> KSPropertyDeclarationDescriptorImpl.getCached(parent)
            else -> throw IllegalStateException()
        }
    }

    override val containingFile: KSFile? = null

    override val modifiers: Set<Modifier> = emptySet()

    override val isReified: Boolean by lazy {
        descriptor.isReified
    }

    override val name: KSName by lazy {
        KSNameImpl.getCached(descriptor.name.asString())
    }

    override val variance: Variance = descriptor.variance.toKSVariance()

    override val annotations: List<KSAnnotation> by lazy {
        descriptor.annotations.map{ KSAnnotationDescriptorImpl.getCached(it) }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeParameter(this, data)
    }
}