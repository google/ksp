/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol

/**
 * A [KSModifierListOwner] can have zero or more modifiers.
 */
interface KSModifierListOwner : KSNode {
    /**
     * The set of modifiers on this element.
     */
    val modifiers: Set<Modifier>
}