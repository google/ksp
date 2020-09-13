/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import com.google.devtools.kotlin.symbol.processing.processing.impl.ResolverImpl
import com.google.devtools.kotlin.symbol.processing.processing.impl.findActualsInKSDeclaration
import com.google.devtools.kotlin.symbol.processing.processing.impl.findExpectsInKSDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSExpectActual
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