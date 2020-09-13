/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol

/**
 * A type parameter
 */
interface KSTypeParameter : KSDeclaration {
    /**
     * Name of the type parameter
     *
     * For example, in `class Foo<T>`, the name value is "T"
     */
    val name: KSName

    /**
     * Declaration-site variance
     */
    val variance: Variance

    /**
     * True if it is reified, i.e., has the reified modifier.
     */
    val isReified: Boolean

    /**
     * Upper bounds of the type parameter.
     */
    val bounds: List<KSTypeReference>
}