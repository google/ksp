/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol

/**
 * A Kotlin source file
 */
interface KSFile : KSDeclarationContainer, KSAnnotated {
    /**
     * The [KSName] representation of this file's package.
     */
    val packageName: KSName

    /**
     * Absolute path of this source file.
     */
    val fileName: String
}