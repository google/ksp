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

    override val parent: KSNode? by lazy {
        var parentPsi = psi.parent
        while (true) {
            when (parentPsi) {
                null, is PsiJavaFile, is PsiClass, is PsiMethod, is PsiParameter, is PsiTypeParameter, is PsiType ->
                    break
                else -> parentPsi = parentPsi.parent
            }
        }
        when (parentPsi) {
            is PsiJavaFile -> KSFileJavaImpl.getCached(parentPsi)
            is PsiClass -> KSClassDeclarationJavaImpl.getCached(parentPsi)
            is PsiMethod -> KSFunctionDeclarationJavaImpl.getCached(parentPsi)
            is PsiParameter -> KSValueParameterJavaImpl.getCached(parentPsi)
            is PsiTypeParameter -> KSTypeParameterJavaImpl.getCached(parentPsi)
            is PsiType ->
                if (parentPsi.parent is PsiClassType) KSTypeArgumentJavaImpl.getCached(parentPsi, this)
                else KSTypeReferenceJavaImpl.getCached(parentPsi, this)
            else -> null
        }
    }

    override val annotationType: KSTypeReference by lazy {
        val psiClass = psi.nameReferenceElement!!.resolve() as? PsiClass
        psiClass?.let {
            (psi.containingFile as? PsiJavaFile)?.let {
                ResolverImpl.instance.incrementalContext.recordLookup(it, psiClass.qualifiedName!!)
            }
        }
        KSTypeReferenceLiteJavaImpl.getCached(psiClass, this)
    }

    override val arguments: List<KSValueArgument> by lazy {
        val annotationConstructor = (
            (annotationType.resolve() as? KSTypeImpl)?.kotlinType?.constructor
                ?.declarationDescriptor as? ClassDescriptor
            )?.constructors?.single()
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
                    value = calculatedValue,
                    this
                )
            }
        val presentValueArgumentNames = presentValueArguments.map { it.name?.asString() ?: "" }
        val argumentsFromDefault = annotationConstructor?.let {
            it.getAbsentDefaultArguments(presentValueArgumentNames, this)
        } ?: emptyList()
        presentValueArguments.plus(argumentsFromDefault)
    }

    /*
     * Don't map Java types in annotation parameters
     *
     * Users may specify Java types explicitly by instances of `Class<T>`.
     * The situation is similar to `getClassDeclarationByName` where we have
     * decided to keep those Java types not mapped.
     *
     * It would be troublesome if users try to use reflection on types that
     * were mapped to Kotlin builtins, becuase some of those builtins don't
     * even exist in classpath.
     *
     * Therefore, ResolverImpl.resolveJavaType cannot be used.
     */
    private fun resolveJavaTypeSimple(psiType: PsiType): KSType {
        return when (psiType) {
            is PsiPrimitiveType -> {
                ResolverImpl.instance.getClassDeclarationByName(psiType.boxedTypeName!!)!!.asStarProjectedType()
            }
            is PsiArrayType -> {
                val componentType = resolveJavaTypeSimple(psiType.componentType)
                val componentTypeRef = ResolverImpl.instance.createKSTypeReferenceFromKSType(componentType)
                val typeArgs = listOf(ResolverImpl.instance.getTypeArgument(componentTypeRef, Variance.INVARIANT))
                ResolverImpl.instance.builtIns.arrayType.replace(typeArgs)
            }
            else -> {
                ResolverImpl.instance.getClassDeclarationByName(psiType.canonicalText)?.asStarProjectedType()
                    ?: KSErrorType
            }
        }
    }

    private fun calcValue(value: PsiAnnotationMemberValue?): Any? {
        if (value is PsiAnnotation) {
            return getCached(value)
        }
        val result = when (value) {
            is PsiReference -> value.resolve()?.let { resolved ->
                JavaPsiFacade.getInstance(value.project).constantEvaluationHelper.computeConstantExpression(value)
                    ?: resolved
            }
            else -> value?.let {
                JavaPsiFacade.getInstance(value.project).constantEvaluationHelper.computeConstantExpression(value)
            }
        }
        return when (result) {
            is PsiType -> {
                resolveJavaTypeSimple(result)
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
                        it is KSClassDeclaration && it.classKind == ClassKind.ENUM_ENTRY &&
                            it.simpleName.asString() == result.name
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
