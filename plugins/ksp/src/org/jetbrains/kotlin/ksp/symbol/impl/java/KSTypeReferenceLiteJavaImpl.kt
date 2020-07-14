/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.KSObjectCache

class KSTypeReferenceLiteJavaImpl private constructor(val type: KSType) : KSTypeReference {
    companion object : KSObjectCache<KSType, KSTypeReferenceLiteJavaImpl>() {
        fun getCached(type: KSType) = cache.getOrPut(type) { KSTypeReferenceLiteJavaImpl(type) }
    }

    override val origin = Origin.JAVA

    override val element: KSReferenceElement? = null

    override val annotations: List<KSAnnotation> = emptyList()

    override val modifiers: Set<Modifier> = emptySet()

    override fun resolve(): KSType? {
        return type
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }
}