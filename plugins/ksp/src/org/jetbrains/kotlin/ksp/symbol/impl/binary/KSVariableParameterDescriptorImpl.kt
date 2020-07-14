/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg

class KSVariableParameterDescriptorImpl private constructor(val descriptor: ValueParameterDescriptor) : KSVariableParameter {
    companion object : KSObjectCache<ValueParameterDescriptor, KSVariableParameterDescriptorImpl>() {
        fun getCached(descriptor: ValueParameterDescriptor) = cache.getOrPut(descriptor) { KSVariableParameterDescriptorImpl(descriptor) }
    }

    override val origin = Origin.CLASS

    override val annotations: List<KSAnnotation> by lazy {
        descriptor.annotations.map { KSAnnotationDescriptorImpl.getCached(it) }
    }

    override val isCrossInline: Boolean = descriptor.isCrossinline

    override val isNoInline: Boolean = descriptor.isNoinline

    override val isVararg: Boolean = descriptor.isVararg

    override val isVal: Boolean = false

    override val isVar: Boolean = false

    override val name: KSName? by lazy {
        KSNameImpl.getCached(descriptor.name.asString())
    }

    override val type: KSTypeReference by lazy {
        KSTypeReferenceDescriptorImpl.getCached(descriptor.type)
    }

    override val hasDefault: Boolean = descriptor.hasDefaultValue()

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitVariableParameter(this, data)
    }
}