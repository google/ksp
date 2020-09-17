/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.processing

import com.google.devtools.ksp.symbol.KSNode

interface KSPLogger {

    fun logging(message: String, symbol: KSNode? = null)
    fun info(message: String, symbol: KSNode? = null)
    fun warn(message: String, symbol: KSNode? = null)
    fun error(message: String, symbol: KSNode? = null)

    fun exception(e: Throwable)
}