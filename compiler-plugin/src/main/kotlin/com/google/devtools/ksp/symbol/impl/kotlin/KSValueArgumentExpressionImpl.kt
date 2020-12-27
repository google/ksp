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

import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toKSExpression
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.psi.KtValueArgument

class KSValueArgumentExpressionImpl private constructor(
    override val index: Int,
    private val ktValueArgument: KtValueArgument
) : KSValueArgumentExpression {
    companion object : KSObjectCache<Pair<Int, KtValueArgument>, KSValueArgumentExpressionImpl>() {
        fun getCached(index: Int, valueArgument: KtValueArgument) =
            cache.getOrPut(Pair(index, valueArgument)) { KSValueArgumentExpressionImpl(index, valueArgument) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktValueArgument.toLocation()
    }

    override val text: String by lazy {
        ktValueArgument.text
    }

    override val name: String? by lazy {
        ktValueArgument.getArgumentName()?.text
    }

    override val isSpread: Boolean by lazy {
        ktValueArgument.isSpread
    }

    override val value: KSExpression? by lazy {
        ktValueArgument.getArgumentExpression()?.toKSExpression()
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is KSValueArgumentExpression)
            return false

        return other.name == this.name && other.value == this.value
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitExpression(this, data)
    }

    override fun toString(): String {
        return (name?.let { "$it = " } ?: "") + value
    }
}