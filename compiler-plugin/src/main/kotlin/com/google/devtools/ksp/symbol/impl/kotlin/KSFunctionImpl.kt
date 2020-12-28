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
package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.KSFunction
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.impl.binary.KSTypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import java.util.*

class KSFunctionImpl(val descriptor: CallableDescriptor) : KSFunction {

    override val isError: Boolean = false

    private val cachedHashCode by lazy(LazyThreadSafetyMode.PUBLICATION) {
        Objects.hash(
            returnType ?: 0,
            parameterTypes,
            typeParameters,
            extensionReceiverType ?: 0
        )
    }

    override val returnType by lazy(LazyThreadSafetyMode.PUBLICATION) {
        descriptor.returnType?.let(::getKSTypeCached)
    }
    override val parameterTypes: List<KSType> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        descriptor.valueParameters.map {
            getKSTypeCached(it.type)
        }
    }
    override val typeParameters: List<KSTypeParameter> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        descriptor.typeParameters.map {
            KSTypeParameterDescriptorImpl.getCached(it)
        }
    }

    override val extensionReceiverType: KSType? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        descriptor.extensionReceiverParameter?.type?.let(::getKSTypeCached)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KSFunctionImpl

        if (returnType != other.returnType) return false
        if (parameterTypes != other.parameterTypes) return false
        if (typeParameters != other.typeParameters) return false
        if (extensionReceiverType != other.extensionReceiverType) return false

        return true
    }

    override fun hashCode() = cachedHashCode

    override fun toString(): String {
        return "KSFunctionImpl(descriptor=$descriptor)"
    }
}
