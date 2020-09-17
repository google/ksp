/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol

/**
 * A declaration container can have a list of declarations declared in it.
 */
interface KSDeclarationContainer : KSNode {
    /**
     * Declarations that are lexically declared inside the current container.
     */
    val declarations: List<KSDeclaration>
}