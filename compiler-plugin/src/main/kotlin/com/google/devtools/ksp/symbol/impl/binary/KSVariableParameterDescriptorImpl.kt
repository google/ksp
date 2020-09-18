/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.google.devtools.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.calls.components.isVararg

class KSVariableParameterDescriptorImpl private constructor(val descriptor: ValueParameterDescriptor) : KSVariableParameter {
    companion object : KSObjectCache<ValueParameterDescriptor, KSVariableParameterDescriptorImpl>() {
        fun getCached(descriptor: ValueParameterDescriptor) = cache.getOrPut(descriptor) { KSVariableParameterDescriptorImpl(descriptor) }
    }

    override val origin = Origin.CLASS

    override val location: Location = NonExistLocation

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

    override fun toString(): String {
        return name?.asString() ?: "_"
    }
}