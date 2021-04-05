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

import com.google.devtools.ksp.findActualType
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.findParentDeclaration
import com.google.devtools.ksp.symbol.impl.toKSModifiers
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.*

abstract class KSDeclarationImpl(ktDeclaration: KtDeclaration) : KSDeclaration {
    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktDeclaration.toLocation()
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(ktDeclaration.name!!)
    }

    override val qualifiedName: KSName? by lazy {
        (ktDeclaration as? KtNamedDeclaration)?.fqName?.let { KSNameImpl.getCached(it.asString()) }
    }

    override val annotations: List<KSAnnotation> by lazy {
        ktDeclaration.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = ktDeclaration.toKSModifiers()
        val hasJvmStatic = when(ktDeclaration) {
            is KtFunction, is KtProperty, is KtPropertyAccessor -> {
                annotations.any {
                    val declaration = it.annotationType.resolve().declaration.let { decl ->
                        if (decl is KSTypeAlias) {
                            decl.findActualType()
                        } else {
                            decl
                        }
                    }
                    declaration.qualifiedName?.asString() == JvmStatic::class.java.canonicalName
                }
            }
            else -> false
        }
        if (hasJvmStatic) {
            modifiers + Modifier.JAVA_STATIC
        } else {
            modifiers
        }
    }
    override val containingFile: KSFile? by lazy {
        KSFileImpl.getCached(ktDeclaration.containingKtFile)
    }

    override val packageName: KSName by lazy {
        this.containingFile!!.packageName
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        (ktDeclaration as? KtTypeParameterListOwner)?.let {
            it.typeParameters.map { KSTypeParameterImpl.getCached(it, ktDeclaration) }
        } ?: emptyList()
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        ktDeclaration.findParentDeclaration()
    }

    override fun toString(): String {
        return this.simpleName.asString()
    }

    internal val originalAnnotations: List<KSAnnotation> by lazy {
        ktDeclaration.annotationEntries.map { KSAnnotationImpl.getCached(it) }
    }
}
