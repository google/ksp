/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotationMethod
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSTypeImpl
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSValueArgumentLiteImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.constants.*

class KSAnnotationDescriptorImpl private constructor(val descriptor: AnnotationDescriptor) : KSAnnotation {
    companion object : KSObjectCache<AnnotationDescriptor, KSAnnotationDescriptorImpl>() {
        fun getCached(descriptor: AnnotationDescriptor) = cache.getOrPut(descriptor) { KSAnnotationDescriptorImpl(descriptor) }
    }

    override val origin = Origin.CLASS

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
    val ksName = KSNameImpl.getCached(this.asSingleFqName().asString())
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

fun AnnotationDescriptor.createKSValueArguments(): List<KSValueArgument> {
    val presentValueArguments = allValueArguments.map { (name, constantValue) ->
        KSValueArgumentLiteImpl.getCached(
            KSNameImpl.getCached(name.asString()),
            constantValue.toValue()
        )
    }
    val presentValueArgumentNames = presentValueArguments.map { it.name.asString() }
    val argumentsFromDefault = (this.type.constructor.declarationDescriptor as? ClassDescriptor)?.constructors?.single()?.let {
        it.getAbsentDefaultArguments(presentValueArgumentNames)
    } ?: emptyList()
    return presentValueArguments.plus(argumentsFromDefault)
}

fun ClassConstructorDescriptor.getAbsentDefaultArguments(excludeNames: Collection<String>): Collection<KSValueArgument> {
    return this.valueParameters.filterNot { param -> excludeNames.contains(param.name.asString()) || !param.hasDefaultValue() }
        .map { param ->
            KSValueArgumentLiteImpl.getCached(
                KSNameImpl.getCached(param.name.asString()),
                param.getDefaultValue()
            )
        }
}

fun ValueParameterDescriptor.getDefaultValue(): Any? {
    val psi = this.findPsi()
    return when (psi) {
        null -> {
            // TODO: This will only work for symbols from Java class.
            ResolverImpl.instance.javaActualAnnotationArgumentExtractor.extractDefaultValue(this, this.type)?.toValue()
        }
        is KtParameter -> ResolverImpl.instance.evaluateConstant(psi.defaultValue, this.type)?.value
        is PsiAnnotationMethod -> JavaPsiFacade.getInstance(psi.project).constantEvaluationHelper.computeConstantExpression((psi).defaultValue)
        else -> throw IllegalStateException()
    }
}
