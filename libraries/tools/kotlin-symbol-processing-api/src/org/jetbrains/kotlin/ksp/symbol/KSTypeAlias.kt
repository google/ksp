/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * A type alias
 */
interface KSTypeAlias : KSDeclaration {
    /**
     * The name of the type alias
     */
    val name: KSName

    /**
     * The reference to the aliased type.
     */
    val type: KSTypeReference
}