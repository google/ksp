/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol

/**
 * A variable parameter
 */
interface KSVariableParameter : KSAnnotated {
    /**
     * Name of the parameter
     */
    val name: KSName?

    /**
     *  The reference to the type of the parameter.
     */
    // TODO: a setter doesn't have a type; However, it can be learned from the corresponding property declaration.
    val type: KSTypeReference?

    /**
     * True if it is a vararg.
     */
    val isVararg: Boolean

    /**
     * True if it has the `noinline` modifier
     */
    val isNoInline: Boolean

    /**
     * True if it has the `crossinline` modifier
     */
    val isCrossInline: Boolean

    /**
     * True if it is a value
     */
    val isVal: Boolean

    /**
     * True if it is a variable
     */
    val isVar: Boolean

    /**
     * True if it has a default value
     */
    val hasDefault: Boolean
}