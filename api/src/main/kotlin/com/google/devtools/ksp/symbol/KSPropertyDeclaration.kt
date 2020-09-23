/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.google.devtools.ksp.symbol

/**
 * A property declaration, can also be used to denote a variable declaration.
 */
interface KSPropertyDeclaration : KSDeclaration {

    /**
     * Getter of the property.
     * Note that when KSPropertyDeclaration is used to model a variable, getter is always null, as a variable can't have a getter.
     */
    val getter: KSPropertyGetter?

    /**
     * Setter of the property.
     * Note that when KSPropertyDeclaration is used to model a variable, setter is always null, as a variable can't have a setter.
     * If a property is immutable, setter is always null as well, as an immutable property can't have a setter.
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
    val type: KSTypeReference

    /**
     * True if this property is mutable.
     */
    val isMutable: Boolean

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

    /**
     * Find the original overridee of this property, if overriding.
     * @return [KSPropertyDeclaration] for the original property, if overriding, otherwise null.
     * Calling [findOverridee] is expensive and should be avoided if possible.
     */
    fun findOverridee(): KSPropertyDeclaration?
}