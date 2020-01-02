/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.binary.createKSValueArguments
import org.jetbrains.kotlin.psi.KtAnnotationEntry

class KSAnnotationImpl(val ktAnnotationEntry: KtAnnotationEntry) : KSAnnotation {
    companion object {
        private val cache = mutableMapOf<KtAnnotationEntry, KSAnnotationImpl>()

        fun getCached(ktAnnotationEntry: KtAnnotationEntry) = cache.getOrPut(ktAnnotationEntry) { KSAnnotationImpl(ktAnnotationEntry) }
    }

    override val annotationType: KSTypeReference by lazy {
        KSTypeReferenceImpl.getCached(ktAnnotationEntry.typeReference!!)
    }

    override val arguments: List<KSValueArgument> by lazy {
        if (ktAnnotationEntry.valueArguments.isEmpty())
            listOf()
        else
            resolved?.createKSValueArguments() ?: listOf()
    }

    override val shortName: KSName by lazy {
        KSNameImpl.getCached(ktAnnotationEntry.shortName!!.asString())
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitAnnotation(this, data)
    }

    private val resolved: AnnotationDescriptor? by lazy {
        ResolverImpl.instance.resolveAnnotationEntry(ktAnnotationEntry)
    }
}
