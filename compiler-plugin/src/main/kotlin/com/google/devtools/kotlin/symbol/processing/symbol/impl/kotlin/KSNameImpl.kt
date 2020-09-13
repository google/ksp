/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin

import com.google.devtools.kotlin.symbol.processing.symbol.KSName
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache

class KSNameImpl private constructor(val name: String) : KSName {
    companion object : KSObjectCache<String, KSNameImpl>() {
        fun getCached(name: String) = cache.getOrPut(name) { KSNameImpl(name) }
    }

    override fun asString(): String {
        return name
    }

    override fun getQualifier(): String {
        return name.split(".").dropLast(1).joinToString(".")
    }

    override fun getShortName(): String {
        return name.split(".").last()
    }
}