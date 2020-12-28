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
import com.google.devtools.ksp.symbol.KSConstantExpression
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression


class KSConstantExpressionImpl private constructor(ktExpression: KtExpression) : KSConstantExpression, KSExpressionImpl(ktExpression) {
    companion object : KSObjectCache<KtExpression, KSConstantExpressionImpl>() {
        fun getCached(ktTypeReference: KtExpression) = cache.getOrPut(ktTypeReference) { KSConstantExpressionImpl(ktTypeReference) }
    }

    override val value: Any? by lazy {
        when(ktExpression) {
            is KtConstantExpression -> ResolverImpl.instance.resolveConstant(ktExpression)
            // from: https://github.com/JetBrains/kotlin/blob/master/compiler/frontend/src/org/jetbrains/kotlin/psi/psiUtil/KtStringTemplateExpressionManipulator.kt#L53-L58
            is KtStringTemplateExpression -> ktExpression.entries.joinToString("") { entry ->
                when (entry) {
                    is KtStringTemplateEntryWithExpression -> entry.text
                    else -> StringUtil.escapeStringCharacters(entry.text)
                }
            }
            else -> null
        }
    }
}