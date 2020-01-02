/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.KSAnnotation
import org.jetbrains.kotlin.ksp.symbol.KSName
import org.jetbrains.kotlin.ksp.symbol.KSValueArgument
import org.jetbrains.kotlin.ksp.symbol.KSVisitor
import org.jetbrains.kotlin.resolve.constants.ConstantValue

class KSValueArgumentLiteImpl(override val name: KSName, override val value: Any?) : KSValueArgumentImpl() {
    companion object {
        private val cache = mutableMapOf<Pair<KSName, Any?>, KSValueArgumentLiteImpl>()

        fun getCached(name: KSName, value: Any?) = cache.getOrPut(Pair(name, value)) { KSValueArgumentLiteImpl(name, value) }
    }

    override val annotations: List<KSAnnotation> = emptyList()
    override val isSpread: Boolean = false

}

abstract class KSValueArgumentImpl() : KSValueArgument {
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
}