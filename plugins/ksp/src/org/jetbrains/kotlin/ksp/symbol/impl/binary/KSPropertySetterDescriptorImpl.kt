/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache

class KSPropertySetterDescriptorImpl private constructor(descriptor: PropertySetterDescriptor) :
    KSPropertyAccessorDescriptorImpl(descriptor), KSPropertySetter {
    companion object : KSObjectCache<PropertySetterDescriptor, KSPropertySetterDescriptorImpl>() {
        fun getCached(descriptor: PropertySetterDescriptor) = cache.getOrPut(descriptor) { KSPropertySetterDescriptorImpl(descriptor) }
    }

    override val parameter: KSVariableParameter by lazy {
        descriptor.valueParameters.singleOrNull()?.let { KSVariableParameterDescriptorImpl.getCached(it) } ?: throw IllegalStateException()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertySetter(this, data)
    }
}