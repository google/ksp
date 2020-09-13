/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache

class KSTypeReferenceDeferredImpl private constructor(private val resolver: () -> KSType?) : KSTypeReference {
    companion object : KSObjectCache<() -> KSType?, KSTypeReferenceDeferredImpl>() {
        fun getCached(resolver: () -> KSType?) = cache.getOrPut(resolver) { KSTypeReferenceDeferredImpl(resolver) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location = NonExistLocation

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

    override fun toString(): String {
        return resolved.toString()
    }
}