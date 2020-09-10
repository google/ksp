/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

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
     * Get all member functions of a class declaration, including declared and inherited.
     * @return List of function declarations from the class members.
     * Calling [getAllFunctions] requires type resolution therefore is expensive and should be avoided if possible.
     */
    fun getAllFunctions(): List<KSFunctionDeclaration>

    /**
     * Create a type by applying a list of type arguments to this class' type parameters.
     * @param typeArguments List of Type arguments to be applied.
     * @return A type constructed from this class declaration with type parameters substituted with the type arguments.
     */
    fun asType(typeArguments: List<KSTypeArgument>): KSType

    /**
     * If this is a generic class, return the type where the type argument is applied with star projection at use-site.
     * @return A type with all type parameters applied with star projection, or same as asUserTypeElement if not generic.
     */
    fun asStarProjectedType(): KSType
}