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

import com.google.devtools.ksp.ExceptionMessage
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotationMethod
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.findPsi
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSAnnotationValueArgumentLiteImpl
import com.google.devtools.ksp.symbol.impl.kotlin.getKSTypeCached
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.constants.*

class KSAnnotationDescriptorImpl private constructor(val descriptor: AnnotationDescriptor) : KSAnnotation {
    companion object : KSObjectCache<AnnotationDescriptor, KSAnnotationDescriptorImpl>() {
        fun getCached(descriptor: AnnotationDescriptor) = cache.getOrPut(descriptor) { KSAnnotationDescriptorImpl(descriptor) }
    }

    override val origin = Origin.CLASS

    override val location: Location = NonExistLocation

    override val annotationType: KSTypeReference by lazy {
        KSTypeReferenceDescriptorImpl.getCached(descriptor.type)
    }

    override val arguments: List<KSAnnotationValueArgument> by lazy {
        descriptor.createKSAnnotationValueArguments()
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(descriptor.fqName!!.shortName().asString())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? = null

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
        it is KSClassDeclaration && it.classKind == ClassKind.ENUM_ENTRY && it.simpleName.asString() == value.second.asString()
    }?.let { (it as KSClassDeclaration).asStarProjectedType() }
    is KClassValue -> when (val classValue = value) {
        is KClassValue.Value.NormalClass -> classValue.classId.findKSType()
        is KClassValue.Value.LocalClass -> getKSTypeCached(classValue.type)
    }
    is ErrorValue, is NullValue -> null
    else -> value
}

fun AnnotationDescriptor.createKSAnnotationValueArguments(): List<KSAnnotationValueArgument> {
    var index = 0
    val presentValueArguments = allValueArguments.map { (name, constantValue) ->
        KSAnnotationValueArgumentLiteImpl.getCached(
            index,
            KSNameImpl.getCached(name.asString()),
            constantValue.toValue()
        ).also { index++ }
    }
    val presentValueArgumentNames = presentValueArguments.map { it.name.asString() }
    val argumentsFromDefault = (this.type.constructor.declarationDescriptor as? ClassDescriptor)?.constructors?.single()?.let {
        it.getAbsentAnnotationDefaultArguments(presentValueArgumentNames)
    } ?: emptyList()
    return presentValueArguments.plus(argumentsFromDefault)
}

fun ClassConstructorDescriptor.getAbsentAnnotationDefaultArguments(excludeNames: Collection<String>): Collection<KSAnnotationValueArgument> {
    return this.valueParameters.filterNot { param -> excludeNames.contains(param.name.asString()) || !param.hasDefaultValue() }
        .mapIndexed { index, param ->
            KSAnnotationValueArgumentLiteImpl.getCached(
                index,
                KSNameImpl.getCached(param.name.asString()),
                param.getDefaultValue()
            )
        }
}

fun ValueParameterDescriptor.getDefaultValue(): Any? {
    return when (val psi = this.findPsi()) {
        null -> {
            // TODO: This will only work for symbols from Java class.
            ResolverImpl.instance.javaActualAnnotationArgumentExtractor.extractDefaultValue(this, this.type)?.toValue()
        }
        is KtParameter -> ResolverImpl.instance.evaluateConstant(psi.defaultValue, this.type)?.value
        is PsiAnnotationMethod -> JavaPsiFacade.getInstance(psi.project).constantEvaluationHelper.computeConstantExpression((psi).defaultValue)
        else -> throw IllegalStateException("Unexpected psi ${psi.javaClass}, $ExceptionMessage")
    }
}
