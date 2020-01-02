/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.toFunctionKSModifiers
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers

class KSPropertySetterDescriptorImpl(val descriptor: PropertySetterDescriptor) : KSPropertySetter {
    companion object {
        private val cache = mutableMapOf<PropertySetterDescriptor, KSPropertySetterDescriptorImpl>()

        fun getCached(descriptor: PropertySetterDescriptor) = cache.getOrPut(descriptor) { KSPropertySetterDescriptorImpl(descriptor) }
    }

    override val annotations: List<KSAnnotation> by lazy {
        descriptor.annotations.map { KSAnnotationDescriptorImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addAll(descriptor.toKSModifiers())
        modifiers.addAll(descriptor.toFunctionKSModifiers())
        modifiers
    }

    override val parameter: KSVariableParameter by lazy {
        descriptor.valueParameters.singleOrNull()?.let { KSVariableParameterDescriptorImpl.getCached(it) } ?: throw IllegalStateException()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertySetter(this, data)
    }
}