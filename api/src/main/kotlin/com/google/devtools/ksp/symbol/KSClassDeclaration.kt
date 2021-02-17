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
 * Models class-like declarations, including class, interface and object.
 */
interface KSClassDeclaration : KSDeclaration, KSDeclarationContainer {

    /**
     * The Kind of the class declaration.
     */
    val classKind: ClassKind

    /**
     * Primary constructor of a class,
     * Secondary constructors can be obtained by filtering [declarations].
     */
    val primaryConstructor: KSFunctionDeclaration?

    /**
     * List of supertypes of this class, containing both super class and implemented interfaces.
     */
    val superTypes: List<KSTypeReference>

    /**
     * Determine whether this class declaration is a companion object.
     * @see [https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html#companion-objects]
     */
    val isCompanionObject: Boolean

    /**
     * @return a sequence of sealed subclasses of this class, if any.
     * Calling [getSealedSubclasses] requires type resolution therefore is expensive and should be avoided if possible.
     */
    fun getSealedSubclasses(): Sequence<KSClassDeclaration>

    /**
     * Get all member functions of a class declaration, including declared and inherited.
     * @return List of function declarations from the class members.
     * Calling [getAllFunctions] requires type resolution therefore is expensive and should be avoided if possible.
     */
    fun getAllFunctions(): List<KSFunctionDeclaration>

    /**
     * Get all member properties of a class declaration, including declared and inherited.
     * @return List of properties declarations from the class members.
     * Calling [getAllProperties] requires type resolution therefore is expensive and should be avoided if possible.
     */
    fun getAllProperties(): List<KSPropertyDeclaration>

    /**
     * Create a type by applying a list of type arguments to this class' type parameters.
     * @param typeArguments List of Type arguments to be applied.
     * @return A type constructed from this class declaration with type parameters substituted with the type arguments.
     */
    fun asType(typeArguments: List<KSTypeArgument>): KSType

    /**
     * If this is a generic class, return the type where the type argument is applied with star projection at use-site.
     * @return A type with all type parameters applied with star projection.
     */
    fun asStarProjectedType(): KSType
}