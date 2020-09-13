/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import com.google.devtools.kotlin.symbol.processing.symbol.KSDeclaration
import com.google.devtools.kotlin.symbol.processing.symbol.KSExpectActual

class KSExpectActualNoImpl : KSExpectActual {
    override val isActual: Boolean = false

    override val isExpect: Boolean = false

    override fun findActuals(): List<KSDeclaration> = emptyList()

    override fun findExpects(): List<KSDeclaration> = emptyList()
}