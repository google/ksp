/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.KSDeclaration
import org.jetbrains.kotlin.ksp.symbol.KSName
import org.jetbrains.kotlin.psi.KtDeclaration

abstract class KSDeclarationImpl() : KSDeclaration {
    override val packageName: KSName by lazy {
        this.containingFile!!.packageName
    }
}