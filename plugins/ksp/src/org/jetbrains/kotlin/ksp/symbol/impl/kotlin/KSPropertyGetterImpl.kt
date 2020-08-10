/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ksp.processing.impl.ResolverImpl
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache
import org.jetbrains.kotlin.ksp.symbol.impl.binary.KSTypeReferenceDescriptorImpl
import org.jetbrains.kotlin.ksp.symbol.impl.toKSModifiers
import org.jetbrains.kotlin.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor

class KSPropertyGetterImpl private constructor(ktPropertyGetter: KtPropertyAccessor) : KSPropertyAccessorImpl(ktPropertyGetter),
    KSPropertyGetter {
    companion object : KSObjectCache<KtPropertyAccessor, KSPropertyGetterImpl>() {
        fun getCached(ktPropertyGetter: KtPropertyAccessor) = cache.getOrPut(ktPropertyGetter) { KSPropertyGetterImpl(ktPropertyGetter) }
    }

    override val returnType: KSTypeReference? by lazy {
        val property = ktPropertyGetter.property
        if (property.typeReference != null) {
            KSTypeReferenceImpl.getCached(property.typeReference!!)
        } else {
            val desc = ResolverImpl.instance.resolveDeclaration(property) as PropertyDescriptor
            KSTypeReferenceDescriptorImpl.getCached(desc.returnType!!)
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitPropertyGetter(this, data)
    }
}