/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol.impl.java

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSName

abstract class KSDeclarationJavaImpl : KSDeclaration {
    override val packageName: KSName by lazy {
        this.containingFile!!.packageName
    }

    override fun toString(): String {
        return this.simpleName.asString()
    }
}