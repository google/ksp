/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin.KSExpectActualNoImpl
import com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin.KSNameImpl
import com.google.devtools.kotlin.symbol.processing.symbol.impl.toKSVariance
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KSTypeParameterDescriptorImpl private constructor(val descriptor: TypeParameterDescriptor) : KSTypeParameter,
    KSDeclarationDescriptorImpl(descriptor),
    KSExpectActual by KSExpectActualNoImpl() {
    companion object : KSObjectCache<TypeParameterDescriptor, KSTypeParameterDescriptorImpl>() {
        fun getCached(descriptor: TypeParameterDescriptor) = cache.getOrPut(descriptor) { KSTypeParameterDescriptorImpl(descriptor) }
    }

    override val bounds: List<KSTypeReference> by lazy {
        descriptor.upperBounds.map { KSTypeReferenceDescriptorImpl.getCached(it) }
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

    override val modifiers: Set<Modifier> = emptySet()

    override val isReified: Boolean by lazy {
        descriptor.isReified
    }

    override val name: KSName by lazy {
        KSNameImpl.getCached(descriptor.name.asString())
    }

    override val variance: Variance = descriptor.variance.toKSVariance()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeParameter(this, data)
    }
}