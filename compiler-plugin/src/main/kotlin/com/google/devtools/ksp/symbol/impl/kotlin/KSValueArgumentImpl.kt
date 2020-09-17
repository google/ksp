/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.kotlin

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache

class KSValueArgumentLiteImpl private constructor(override val name: KSName, override val value: Any?) : KSValueArgumentImpl() {
    companion object : KSObjectCache<Pair<KSName, Any?>, KSValueArgumentLiteImpl>() {
        fun getCached(name: KSName, value: Any?) = cache.getOrPut(Pair(name, value)) { KSValueArgumentLiteImpl(name, value) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location = NonExistLocation

    override val annotations: List<KSAnnotation> = emptyList()

    override val isSpread: Boolean = false
}

abstract class KSValueArgumentImpl : KSValueArgument {
    override fun hashCode(): Int {
        return name.hashCode() * 31 + value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KSValueArgument)
            return false

        return other.name == this.name && other.value == this.value
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitValueArgument(this, data)
    }

    override fun toString(): String {
        return "${name?.asString() ?: ""}:${value.toString()}"
    }
}