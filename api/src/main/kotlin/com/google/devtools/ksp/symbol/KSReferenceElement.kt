/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol

/**
 * An application/reference to a type declared somewhere else.
 *
 * KSReferenceElement can specify, for example, a class, interface, or function, etc.
 */
interface KSReferenceElement : KSNode {
    /**
     * Type arguments in the type reference.
     */
    val typeArguments: List<KSTypeArgument>
}
