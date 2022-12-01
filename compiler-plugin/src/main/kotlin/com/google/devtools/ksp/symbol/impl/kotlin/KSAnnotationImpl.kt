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

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.binary.createKSValueArguments
import com.google.devtools.ksp.symbol.impl.binary.getDefaultArguments
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.*
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference

class KSAnnotationImpl private constructor(val ktAnnotationEntry: KtAnnotationEntry) : KSAnnotation {
    companion object : KSObjectCache<KtAnnotationEntry, KSAnnotationImpl>() {
        fun getCached(ktAnnotationEntry: KtAnnotationEntry) = cache.getOrPut(ktAnnotationEntry) {
            KSAnnotationImpl(ktAnnotationEntry)
        }
    }

    override val origin = Origin.KOTLIN

    override val parent: KSNode? by lazy {
        var parentPsi = ktAnnotationEntry.parent
        while (true) {
            when (parentPsi) {
                null, is KtFile, is KtClassOrObject, is KtFunction, is KtParameter, is KtTypeParameter,
                is KtTypeAlias, is KtProperty, is KtPropertyAccessor, is KtTypeProjection, is KtTypeReference -> break
                else -> parentPsi = parentPsi.parent
            }
        }
        when (parentPsi) {
            is KtFile -> KSFileImpl.getCached(parentPsi)
            is KtClassOrObject -> KSClassDeclarationImpl.getCached(parentPsi)
            is KtFunction -> KSFunctionDeclarationImpl.getCached(parentPsi)
            is KtParameter -> KSValueParameterImpl.getCached(parentPsi)
            is KtTypeParameter -> KSTypeParameterImpl.getCached(parentPsi)
            is KtTypeAlias -> KSTypeAliasImpl.getCached(parentPsi)
            is KtProperty -> KSPropertyDeclarationImpl.getCached(parentPsi)
            is KtPropertyAccessor -> KSPropertyAccessorImpl.getCached(parentPsi)
            is KtTypeProjection -> KSTypeArgumentKtImpl.getCached(parentPsi)
            is KtTypeReference -> KSTypeReferenceImpl.getCached(parentPsi)
            else -> null
        }
    }

    override val location: Location by lazy {
        ktAnnotationEntry.toLocation()
    }

    override val annotationType: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktAnnotationEntry.typeReference!!)
    }

    override val arguments: List<KSValueArgument> by lazy {
        resolved?.createKSValueArguments(this) ?: emptyList()
    }

    override val defaultArguments: List<KSValueArgument> by lazy {
        resolved?.getDefaultArguments(this) ?: emptyList()
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(ktAnnotationEntry.shortName!!.asString())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? by lazy {
        when (ktAnnotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()) {
            null -> null
            FILE -> AnnotationUseSiteTarget.FILE
            PROPERTY -> AnnotationUseSiteTarget.PROPERTY
            FIELD -> AnnotationUseSiteTarget.FIELD
            PROPERTY_GETTER -> AnnotationUseSiteTarget.GET
            PROPERTY_SETTER -> AnnotationUseSiteTarget.SET
            RECEIVER -> AnnotationUseSiteTarget.RECEIVER
            CONSTRUCTOR_PARAMETER -> AnnotationUseSiteTarget.PARAM
            SETTER_PARAMETER -> AnnotationUseSiteTarget.SETPARAM
            PROPERTY_DELEGATE_FIELD -> AnnotationUseSiteTarget.DELEGATE
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    private val resolved: AnnotationDescriptor? by lazy {
        ResolverImpl.instance!!.resolveAnnotationEntry(ktAnnotationEntry)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}
