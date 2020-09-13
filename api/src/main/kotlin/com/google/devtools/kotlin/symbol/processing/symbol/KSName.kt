/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol

/**
 * Kotlin Symbol Processing's representation of names. Can be simple or qualified names.
 */
interface KSName {
    /**
     * String representation of the name.
     */
    fun asString(): String

    /**
     * Qualifier of the name.
     */
    fun getQualifier(): String

    /**
     * If a qualified name, it is the last part. Otherwise it is the same as [asString]
     */
    fun getShortName(): String
}