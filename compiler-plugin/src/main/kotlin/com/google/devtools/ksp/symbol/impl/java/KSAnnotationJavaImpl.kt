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

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.binary.getAbsentDefaultArguments
import com.google.devtools.ksp.symbol.impl.kotlin.KSErrorType
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSTypeImpl
import com.google.devtools.ksp.symbol.impl.toLocation
import com.intellij.lang.jvm.JvmClassKind
import com.intellij.psi.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor

class KSAnnotationJavaImpl private constructor(val psi: PsiAnnotation) : KSAnnotation {
    companion object : KSObjectCache<PsiAnnotation, KSAnnotationJavaImpl>() {
        fun getCached(psi: PsiAnnotation) = cache.getOrPut(psi) { KSAnnotationJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val location: Location by lazy {
        psi.toLocation()
    }

    override val annotationType: KSTypeReference by lazy {
        val psiClass = psi.nameReferenceElement!!.resolve() as? PsiClass ?: return@lazy KSTypeReferenceLiteJavaImpl.getCached(KSErrorType)
        (psi.containingFile as? PsiJavaFile)?.let {
            ResolverImpl.instance.incrementalContext.recordLookup(it, psiClass.qualifiedName!!)
        }
        KSTypeReferenceLiteJavaImpl.getCached(
            KSClassDeclarationJavaImpl.getCached(psiClass).asType(emptyList())
        )
    }

    override val arguments: List<KSValueArgument> by lazy {
        val annotationConstructor =
            ((annotationType.resolve() as KSTypeImpl).kotlinType.constructor.declarationDescriptor as? ClassDescriptor)
                ?.constructors?.single()
        val presentValueArguments = psi.parameterList.attributes
            .mapIndexed { index, it ->
                // use the name in the attribute if it is explicitly specified, otherwise, fall back to index.
                val name = it.name ?: annotationConstructor?.valueParameters?.getOrNull(index)?.name?.asString()
                val value = it.value
                val calculatedValue: Any? = if (value is PsiArrayInitializerMemberValue) {
                    value.initializers.map {
                        calcValue(it)
                    }
                } else {
                    calcValue(it.value)
                }
                KSValueArgumentJavaImpl.getCached(
                    name = name?.let(KSNameImpl::getCached),
                    value = calculatedValue
                )
            }
        val presentValueArgumentNames = presentValueArguments.map { it.name?.asString() ?: "" }
        val argumentsFromDefault = annotationConstructor?.let {
            it.getAbsentDefaultArguments(presentValueArgumentNames)
        } ?: emptyList()
        presentValueArguments.plus(argumentsFromDefault)
    }

    private fun calcValue(value: PsiAnnotationMemberValue?): Any? {
        if (value is PsiAnnotation) {
            return getCached(value)
        }
        val result = when(value) {
            is PsiReference -> value.resolve()?.let { resolved ->
                JavaPsiFacade.getInstance(value.project).constantEvaluationHelper.computeConstantExpression(value) ?: resolved
            }
            else -> value?.let { JavaPsiFacade.getInstance(value.project).constantEvaluationHelper.computeConstantExpression(value) }
        }
        return when(result) {
            is PsiType -> {
                ResolverImpl.instance.getClassDeclarationByName(result.canonicalText)?.asStarProjectedType() ?: KSErrorType
            }
            is PsiLiteralValue -> {
                result.value
            }
            is PsiField -> {
                // manually handle enums as constant expression evaluator does not seem to be resolving them.
                val containingClass = result.containingClass
                if (containingClass?.classKind == JvmClassKind.ENUM) {
                    // this is an enum entry
                    containingClass.qualifiedName?.let {
                        ResolverImpl.instance.getClassDeclarationByName(it)
                    }?.declarations?.find {
                        it is KSClassDeclaration && it.classKind == ClassKind.ENUM_ENTRY && it.simpleName.asString() == result.name
                    }?.let { (it as KSClassDeclaration).asStarProjectedType() }
                        ?.let {
                            return it
                        }
                } else {
                    null
                }
            }
            else -> result
        }
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
