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

import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert

open class KSExpressionImpl(val ktExpression: KtExpression) : KSExpression {
    companion object : KSObjectCache<KtExpression, KSExpressionImpl>() {
        fun getCached(expression: KtExpression) = cache.getOrPut(expression) { KSExpressionImpl(expression) }
    }

    override val origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktExpression.toLocation()
    }

    override val text: String by lazy {
        ktExpression.text
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        TODO()
    }

    override fun resolve(): KSType = KSTypeImpl.getCached(
        ResolverImpl.instance.bindingTrace.getType(ktExpression)!!
    )

    override fun toString(): String = text
}