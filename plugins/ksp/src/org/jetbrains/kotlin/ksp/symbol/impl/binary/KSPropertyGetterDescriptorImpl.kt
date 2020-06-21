/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.toFunctionKSModifiers
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers

class KSPropertyGetterDescriptorImpl(val descriptor: PropertyGetterDescriptor) : KSPropertyGetter {
    companion object {
        private val cache = mutableMapOf<PropertyGetterDescriptor, KSPropertyGetterDescriptorImpl>()

        fun getCached(descriptor: PropertyGetterDescriptor) = cache.getOrPut(descriptor) { KSPropertyGetterDescriptorImpl(descriptor) }
    }

    override val origin = Origin.CLASS

    override val annotations: List<KSAnnotation> by lazy {
        descriptor.annotations.map { KSAnnotationDescriptorImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addAll(descriptor.toKSModifiers())
        modifiers.addAll(descriptor.toFunctionKSModifiers())
        modifiers
    }

    override val returnType: KSTypeReference? by lazy {
        if (descriptor.returnType != null) {
            KSTypeReferenceDescriptorImpl.getCached(descriptor.returnType!!)
        } else {
            null
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyGetter(this, data)
    }
}