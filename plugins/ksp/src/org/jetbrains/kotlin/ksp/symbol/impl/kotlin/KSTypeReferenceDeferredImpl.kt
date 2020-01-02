/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*

class KSTypeReferenceDeferredImpl(private val resolver: () -> KSType?) : KSTypeReference {
    companion object {
        private val cache = mutableMapOf<() -> KSType?, KSTypeReferenceDeferredImpl>()

        fun getCached(resolver: () -> KSType?) = cache.getOrPut(resolver) { KSTypeReferenceDeferredImpl(resolver) }
    }

    override val annotations: List<KSAnnotation> = emptyList()
    override val element: KSReferenceElement? = null
    override val modifiers: Set<Modifier> = emptySet()

    private val resolved: KSType? by lazy {
        resolver()
    }

    override fun resolve(): KSType? = resolved

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }
}