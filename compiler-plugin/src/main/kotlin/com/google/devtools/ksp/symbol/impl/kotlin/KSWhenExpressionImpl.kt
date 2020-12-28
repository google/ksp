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
import org.jetbrains.kotlin.psi.*

class KSWhenExpressionImpl private constructor(ktWhenExpression: KtWhenExpression) : KSWhenExpression,
    KSExpressionImpl(ktWhenExpression) {
    companion object : KSObjectCache<KtWhenExpression, KSWhenExpressionImpl>() {
        fun getCached(ktWhenExpression: KtWhenExpression) =
            cache.getOrPut(ktWhenExpression) { KSWhenExpressionImpl(ktWhenExpression) }
    }

    override val branches: List<KSWhenExpression.Branch> by lazy {
        ktWhenExpression.entries.map(Branch::getCached)
    }

    override val subject: KSExpression? by lazy {
        ktWhenExpression.subjectExpression?.toKSExpression()
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitWhenExpression(this, data)
    }

    class Branch private constructor(ktWhenEntry: KtWhenEntry) : KSWhenExpression.Branch, KSExpression {
        companion object : KSObjectCache<KtWhenEntry, Branch>() {
            fun getCached(ktWhenEntry: KtWhenEntry) = cache.getOrPut(ktWhenEntry) { Branch(ktWhenEntry) }
        }

        override val conditions: List<KSExpression> by lazy {
            ktWhenEntry.conditions.map {
                when(it) {
                    is KtWhenConditionInRange -> RangeCondition.getCached(it)
                    is KtWhenConditionIsPattern -> TypeCondition.getCached(it)
                    else -> (it as? KtWhenConditionWithExpression)?.expression?.toKSExpression()
                        ?: error("An unexpected conditional expression was found: ${it.text}")
                }
            }
        }

        override val body: KSExpression by lazy {
            ktWhenEntry.expression?.toKSExpression()
                ?: error("Unable to get conditional branching behavior from ${ktWhenEntry.text}.")
        }

        override val isElse: Boolean by lazy {
            ktWhenEntry.isElse
        }

        override val origin = Origin.KOTLIN

        override val location: Location by lazy {
            ktWhenEntry.toLocation()
        }

        override val text: String by lazy {
            ktWhenEntry.text
        }

        override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
            return visitor.visitWhenExpressionBranch(this, data)
        }

        override fun toString(): String = text


        open class Condition(ktWhenCondition: KtWhenCondition) : KSExpression {
            companion object : KSObjectCache<KtWhenCondition, Condition>() {
                fun getCached(ktWhenCondition: KtWhenCondition) =
                    cache.getOrPut(ktWhenCondition) { Condition(ktWhenCondition) }
            }

            override val text: String by lazy {
                ktWhenCondition.text
            }

            override val origin = Origin.KOTLIN

            override val location: Location by lazy {
                ktWhenCondition.toLocation()
            }

            override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
                return visitor.visitExpression(this, data)
            }

            override fun toString(): String = text
        }

        class RangeCondition private constructor(ktWhenCondition: KtWhenConditionInRange) :
            KSWhenExpression.Branch.RangeCondition, Condition(ktWhenCondition) {
            companion object : KSObjectCache<KtWhenConditionInRange, RangeCondition>() {
                fun getCached(ktWhenCondition: KtWhenConditionInRange) = cache.getOrPut(ktWhenCondition) { RangeCondition(ktWhenCondition) }
            }

            override val operator: KSToken.Operator by lazy {
                if (ktWhenCondition.isNegated) {
                    KSToken.Operator.NotIn
                } else {
                    KSToken.Operator.In
                }
            }

            override val range: KSBinaryExpression by lazy {
                (ktWhenCondition.rangeExpression as? KtBinaryExpression)?.let(KSBinaryExpressionImpl::getCached)
                    ?: error("Could not get the range expression for the ${ktWhenCondition.text}")
            }
        }

        class TypeCondition private constructor(ktWhenCondition: KtWhenConditionIsPattern) :
            KSWhenExpression.Branch.TypeCondition, Condition(ktWhenCondition) {
            companion object : KSObjectCache<KtWhenCondition, TypeCondition>() {
                fun getCached(ktWhenCondition: KtWhenConditionIsPattern) = cache.getOrPut(ktWhenCondition) { TypeCondition(ktWhenCondition) }
            }

            override val operator: KSToken.Operator by lazy {
                if (ktWhenCondition.isNegated) {
                    KSToken.Operator.NotIs
                } else {
                    KSToken.Operator.Is
                }
            }

            override val type: KSTypeReference by lazy {
                if (ktWhenCondition.typeReference != null) {
                    KSTypeReferenceImpl.getCached(ktWhenCondition.typeReference!!)
                } else {
                    KSTypeReferenceDeferredImpl.getCached { KSErrorType }
                }
            }
        }
    }
}