/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol

/**
 * A type argument
 */
interface KSTypeArgument : KSAnnotated {
    /**
     * The use-site variance, or namely type projection
     */
    val variance: Variance

    /**
     * The reference to the type
     */
    val type: KSTypeReference?
}