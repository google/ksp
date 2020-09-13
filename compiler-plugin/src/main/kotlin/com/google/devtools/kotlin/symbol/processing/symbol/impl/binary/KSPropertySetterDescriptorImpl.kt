/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache

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

    override fun toString(): String {
        return "$receiver.setter()"
    }
}