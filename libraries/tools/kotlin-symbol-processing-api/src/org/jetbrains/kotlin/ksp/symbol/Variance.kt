/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * Represents both declaration-site and use-site variance.
 * STAR is only used and valid in use-site variance, while others can be used in both.
 */
enum class Variance(val label: String) {
    STAR("*"),
    INVARIANT(""),
    COVARIANT("out"),
    CONTRAVARIANT("in");
}