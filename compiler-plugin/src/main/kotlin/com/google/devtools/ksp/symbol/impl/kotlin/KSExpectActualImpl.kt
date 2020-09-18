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

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.processing.impl.findActualsInKSDeclaration
import com.google.devtools.ksp.processing.impl.findExpectsInKSDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSExpectActual
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier

class KSExpectActualImpl(val declaration: KtDeclaration) : KSExpectActual {
    /**
     * "All actual declarations that match any part of an expected declaration need to be marked as actual."
     */
    override val isActual: Boolean = declaration.hasActualModifier()

    private fun KtDeclaration.isExpect(): Boolean = hasExpectModifier() || containingClassOrObject?.isExpect() == true

    override val isExpect: Boolean = declaration.isExpect()

    private val expects: List<KSDeclaration> by lazy {
        descriptor?.findExpectsInKSDeclaration() ?: emptyList()
    }

    override fun findExpects(): List<KSDeclaration> {
        if (!isActual)
            return emptyList()
        return expects
    }

    private val actuals: List<KSDeclaration> by lazy {
        descriptor?.findActualsInKSDeclaration() ?: emptyList()
    }

    override fun findActuals(): List<KSDeclaration> {
        if (!isExpect)
            return emptyList()
        return actuals
    }

    private val descriptor: DeclarationDescriptor? by lazy {
        ResolverImpl.instance.resolveDeclaration(declaration)
    }
}