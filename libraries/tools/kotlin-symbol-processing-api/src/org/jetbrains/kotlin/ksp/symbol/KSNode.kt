/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * Base class of every visitable program elements.
 */
interface KSNode {
    fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R
}