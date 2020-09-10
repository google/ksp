/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * A value argument to function / constructor calls.
 *
 * Currently, only appears in annotation arguments.
 */
interface KSValueArgument : KSAnnotated {
    /**
     * The name for the named argument, or null otherwise.
     *
     * For example, in `ignore(name=123456)`, the name value is "name"
     */
    val name: KSName?

    /**
     * True if it is a spread argument (i.e., has a "*" in front of the argument).
     */
    val isSpread: Boolean

    /**
     * The value of the argument.
     */
    val value: Any?
}