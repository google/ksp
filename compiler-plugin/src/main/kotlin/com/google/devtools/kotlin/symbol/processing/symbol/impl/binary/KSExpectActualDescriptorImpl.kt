/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.MemberDescriptor
import com.google.devtools.kotlin.symbol.processing.processing.impl.findActualsInKSDeclaration
import com.google.devtools.kotlin.symbol.processing.processing.impl.findExpectsInKSDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSExpectActual

class KSExpectActualDescriptorImpl(val descriptor: MemberDescriptor) : KSExpectActual {
    override val isExpect: Boolean = descriptor.isExpect

    override val isActual: Boolean = descriptor.isActual

    private val expects: List<KSDeclaration> by lazy {
        descriptor.findExpectsInKSDeclaration()
    }

    override fun findExpects(): List<KSDeclaration> {
        if (!isActual)
            return emptyList()
        return expects
    }

    private val actuals: List<KSDeclaration> by lazy {
        descriptor.findActualsInKSDeclaration()
    }

    override fun findActuals(): List<KSDeclaration> {
        if (!isExpect)
            return emptyList()
        return actuals
    }
}
