/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSTypeImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSValueArgumentLiteImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

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

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}

private fun ClassId.findKSClassDeclaration(): KSClassDeclaration? {
    val ksName = KSNameImpl(this.asSingleFqName().asString())
    return ResolverImpl.instance.getClassDeclarationByName(ksName)
}

private fun ClassId.findKSType(): KSType? = findKSClassDeclaration()?.asStarProjectedType()

private fun <T> ConstantValue<T>.toValue(): Any? = when (this) {
    is AnnotationValue -> KSAnnotationDescriptorImpl.getCached(value)
    is ArrayValue -> value.map { it.toValue() }
    is EnumValue -> value.first.findKSClassDeclaration()?.declarations?.find {
        it is KSEnumEntryDeclaration && it.simpleName.asString() == value.second.asString()
    }?.let { (it as KSClassDeclaration).asStarProjectedType() }
    is KClassValue -> when (val classValue = value) {
        is KClassValue.Value.NormalClass -> classValue.classId.findKSType()
        is KClassValue.Value.LocalClass -> KSTypeImpl.getCached(classValue.type)
    }
    is ErrorValue, is NullValue -> null
    else -> value
}

fun AnnotationDescriptor.createKSValueArguments(): List<KSValueArgument> =
    allValueArguments.map { (name, constantValue) ->
        KSValueArgumentLiteImpl.getCached(
            KSNameImpl.getCached(name.asString()),
            constantValue.toValue()
        )
    }
