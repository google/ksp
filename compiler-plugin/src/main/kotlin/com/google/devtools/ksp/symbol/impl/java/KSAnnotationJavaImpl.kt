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


package com.google.devtools.ksp.symbol.impl.java

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.binary.getAbsentDefaultArguments
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeImpl
import com.google.devtools.ksp.symbol.impl.toLocation

class KSAnnotationJavaImpl private constructor(val psi: PsiAnnotation) : KSAnnotation {
    companion object : KSObjectCache<PsiAnnotation, KSAnnotationJavaImpl>() {
        fun getCached(psi: PsiAnnotation) = cache.getOrPut(psi) { KSAnnotationJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val location: Location by lazy {
        psi.toLocation()
    }

    override val annotationType: KSTypeReference by lazy {
        KSTypeReferenceLiteJavaImpl.getCached(
            KSClassDeclarationJavaImpl.getCached(psi.nameReferenceElement!!.resolve() as PsiClass).asType(emptyList())
        )
    }

    override val arguments: List<KSValueArgument> by lazy {
        val annotationConstructor =
            ((annotationType.resolve() as KSTypeImpl).kotlinType.constructor.declarationDescriptor as? ClassDescriptor)
                ?.constructors?.single()
        val presentValueArguments = psi.parameterList.attributes
            .mapIndexed { index, it ->
                KSValueArgumentJavaImpl.getCached(
                    annotationConstructor?.valueParameters?.get(index)?.name?.let { KSNameImpl.getCached(it.asString()) },
                    calcValue(it.value)
                )
            }
        val presentValueArgumentNames = presentValueArguments.map { it.name?.asString() ?: "" }
        val argumentsFromDefault = annotationConstructor?.let {
            it.getAbsentDefaultArguments(presentValueArgumentNames)
        } ?: emptyList()
        presentValueArguments.plus(argumentsFromDefault)
    }

    private fun calcValue(value: PsiAnnotationMemberValue?): Any? {
        return value?.let { JavaPsiFacade.getInstance(value.project).constantEvaluationHelper.computeConstantExpression(value) }
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(psi.qualifiedName!!.split(".").last())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? = null

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}