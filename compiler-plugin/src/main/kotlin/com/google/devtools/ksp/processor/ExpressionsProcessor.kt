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


package com.google.devtools.ksp.processor

import com.google.devtools.ksp.firstInitializerBlock
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.kotlin.KSPropertyDeclarationImpl
import com.google.devtools.ksp.visitor.KSTopDownVisitor

class ExpressionsProcessor : AbstractTestProcessor() {
    val results = mutableListOf<String>()
    val visitor = Visitor()

    override fun process(resolver: Resolver) {
        resolver.getClassDeclarationByName("Expressions")?.firstInitializerBlock?.accept(visitor, Unit)
    }

    override fun toResult(): List<String> {
        return results
    }

    private fun addResult(description: String, message: Any?) {
        /**
         * @see KSPropertyDeclarationImpl.shouldCreateSyntheticAccessor
         */
        if (message == "KSPropertyAccessor") return

        results += "${description.lines().let { 
            if (it.size > 1) {
                it.first() + " .. " + description.last()
            } else {
                it.first()
            }
        }}  =>  ${message?.toString() ?: "<Unknown Expression>"}"
    }

    inner class Visitor : KSTopDownVisitor<Unit, Unit>() {
        override fun defaultHandler(node: KSNode, data: Unit) = Unit

        override fun visitExpression(expression: KSExpression, data: Unit) {
            addResult(expression.text, expression.getName())
            super.visitExpression(expression, data)
        }

        override fun visitWhenExpressionBranch(whenBranch: KSWhenExpression.Branch, data: Unit) {
            addResult(whenBranch.toString(), "KSWhenExpression.Branch, isElse: ${whenBranch.isElse}")
            super.visitWhenExpressionBranch(whenBranch, data)
        }

        private fun KSExpression.getName() = when(this) {
            is KSAnonymousInitializer -> "KSAnonymousInitializer"
            is KSPropertyAccessor -> "KSPropertyAccessor"
            is KSClassDeclaration -> "KSClassDeclaration"
            is KSPropertyDeclaration -> "KSPropertyDeclaration"
            is KSFunctionDeclaration -> "KSFunctionDeclaration"
            is KSChainCallsExpression -> "KSChainCallsExpression"
            is KSDslExpression -> "KSDslExpression"
            is KSCallExpression -> "KSCallExpression"
            is KSWhenExpression -> "KSWhenExpression"
            is KSLambdaExpression -> "KSLambdaExpression"
            is KSBlockExpression -> "KSBlockExpression"
            is KSLabeledExpression -> "KSLabeledExpression"
            is KSJumpExpression -> "KSJumpExpression"
            is KSLabelReferenceExpression -> "KSLabelReferenceExpression"
            is KSBinaryExpression -> "KSBinaryExpression"
            is KSUnaryExpression -> "KSUnaryExpression"
            is KSIfExpression -> "KSIfExpression"
            is KSConstantExpression -> "KSConstantExpression"
            is KSTypeCastExpression -> "KSTypeCastExpression"
            is KSValueArgumentExpression -> "KSValueArgumentExpression"
            else -> null
        }
    }
}

