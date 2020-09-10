/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 *  Classes, functions, properties and typealiases can be declared as `expect` in common modules and `actual` in platform modules.
 *
 *  See https://kotlinlang.org/docs/reference/platform-specific-declarations.html for more information.
 */
interface KSExpectActual {
    /**
     * True if this is an `actual` implementation.
     */
    val isActual: Boolean

    /**
     * True if this is an `expect` declaration.
     */
    val isExpect: Boolean

    /**
     * Finds all corresponding `actual` implementations for `this`.
     *
     * @return a list of corresponding `actual` implementations, or an empty list if not applicable.
     */
    fun findActuals(): List<KSDeclaration>

    /**
     * Finds all corresponding `expect` declarations for `this`.
     *
     * @return a list of corresponding `expect` implementations, or an empty list if not applicable.
     */
    fun findExpects(): List<KSDeclaration>
}