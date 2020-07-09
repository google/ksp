/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * A property declaration, can also be used to denote a variable declaration.
 */
interface KSPropertyDeclaration : KSDeclaration {

    /**
     * Getter of the property.
     * Can be null if not declared. Note that when KSPropertyDeclaration is used to model a variable, getter is always null, as a variable can't have a getter.
     */
    val getter: KSPropertyGetter?

    /**
     * Setter of the property.
     * Can be null if not declared. Note that when KSPropertyDeclaration is used to model a variable, setter is always null, as a variable can't have a setter.
     */
    val setter: KSPropertySetter?

    /**
     * Extension receiver if this declaration is an [extension property][https://kotlinlang.org/docs/reference/extensions.html#extension-properties].
     * Dispatch receiver is [parentDeclaration], if any.
     */
    val extensionReceiver: KSTypeReference?

    /**
     * The type of this declaration.
     */
    val type: KSTypeReference?

    /**
     * Indicates whether this is a delegated property.
     */
    fun isDelegated(): Boolean

    /**
     * Checks if this property overrides another property.
     * @param overridee the candidate overridden property being checked.
     * @return boolean value indicating whether this function overrides [overridee]
     * Calling [overrides] is expensive and should be avoided if possible.
     */
    fun overrides(overridee: KSPropertyDeclaration): Boolean
}