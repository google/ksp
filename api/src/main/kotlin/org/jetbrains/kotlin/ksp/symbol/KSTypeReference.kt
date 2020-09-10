/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * A [KSTypeReference] is a [KSReferenceElement] with annotations and modifiers.
 */
interface KSTypeReference : KSAnnotated, KSModifierListOwner {

    /**
     * Underlying element of this type reference, without annotations and modifiers.
     */
    val element: KSReferenceElement?

    /**
     * Resolves to the original declaration site.
     * @return A type resolved from this type reference.
     * Calling [resolve] is expensive and should be avoided if possible.
     */
    fun resolve(): KSType?
}