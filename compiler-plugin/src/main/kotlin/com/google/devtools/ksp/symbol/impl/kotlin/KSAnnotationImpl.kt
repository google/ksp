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

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.binary.createKSValueArguments
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.KtAnnotationEntry

class KSAnnotationImpl private constructor(val ktAnnotationEntry: KtAnnotationEntry) : KSAnnotation {
    companion object : KSObjectCache<KtAnnotationEntry, KSAnnotationImpl>() {
        fun getCached(ktAnnotationEntry: KtAnnotationEntry) = cache.getOrPut(ktAnnotationEntry) { KSAnnotationImpl(ktAnnotationEntry) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktAnnotationEntry.toLocation()
    }

    override val annotationType: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktAnnotationEntry.typeReference!!)
    }

    override val arguments: List<KSValueArgument> by lazy {
        resolved?.createKSValueArguments() ?: listOf()
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(ktAnnotationEntry.shortName!!.asString())
    }

    override val useSiteTarget: AnnotationUseSiteTarget? by lazy {
        when (ktAnnotationEntry.useSiteTarget?.getAnnotationUseSiteTarget()) {
            null -> null
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.FILE -> AnnotationUseSiteTarget.FILE
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY -> AnnotationUseSiteTarget.PROPERTY
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.FIELD -> AnnotationUseSiteTarget.FIELD
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY_GETTER -> AnnotationUseSiteTarget.GET
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY_SETTER -> AnnotationUseSiteTarget.SET
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.RECEIVER -> AnnotationUseSiteTarget.RECEIVER
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> AnnotationUseSiteTarget.PARAM
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.SETTER_PARAMETER -> AnnotationUseSiteTarget.SETPARAM
            org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD -> AnnotationUseSiteTarget.DELEGATE
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    private val resolved: AnnotationDescriptor? by lazy {
        ResolverImpl.instance.resolveAnnotationEntry(ktAnnotationEntry)
    }

    override fun toString(): String {
        return "@${shortName.asString()}"
    }
}
