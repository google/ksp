/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSValueArgumentLiteImpl
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue

class KSAnnotationDescriptorImpl(val descriptor: AnnotationDescriptor) : KSAnnotation {
    companion object {
        private val cache = mutableMapOf<AnnotationDescriptor, KSAnnotationDescriptorImpl>()

        fun getCached(descriptor: AnnotationDescriptor) = cache.getOrPut(descriptor) { KSAnnotationDescriptorImpl(descriptor) }
    }

    override val annotationType: KSTypeReference by lazy {
        KSTypeReferenceDescriptorImpl.getCached(descriptor.type)
    }

    override val arguments: List<KSValueArgument> by lazy {
        descriptor.createKSValueArguments()
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(descriptor.fqName!!.shortName().asString())
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }
}

/**
 * Type information will be lost in KSValueArgument anyway.
 */
fun <T> ConstantValue<T>.toValue(): Any? {
    return when (this) {
        is ArrayValue -> value.map { it.toValue() }
        else -> value
    }
}

fun AnnotationDescriptor.createKSValueArguments(): List<KSValueArgument> =
    allValueArguments.map { (name, constantValue) ->
        val value = constantValue.toValue()
        KSValueArgumentLiteImpl.getCached(
            KSNameImpl.getCached(name.asString()),
            when (value) {
                is AnnotationDescriptor -> KSAnnotationDescriptorImpl.getCached(value)
                // TODO: KClass
                else -> value
            }
        )
    }
