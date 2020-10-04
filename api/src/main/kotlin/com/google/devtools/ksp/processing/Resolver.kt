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


package com.google.devtools.ksp.processing

import com.google.devtools.ksp.symbol.*

/**
 * [Resolver] provides [SymbolProcessor] with access to compiler details such as Symbols.
 */
interface Resolver {
    /**
     * Get all files in the module / compilation unit.
     *
     * @return files in the module.
     */
    fun getAllFiles(): List<KSFile>

    /**
     * Get all symbols with specified annotation.
     *
     * @param annotationName is the full qualified name of the annotation; using '.' as separator.
     * @return Elements annotated with the specified annotation.
     */
    fun getSymbolsWithAnnotation(annotationName: String): List<KSAnnotated>

    /**
     * Find a class in the compilation classpath for the given name.
     *
     * @param name fully qualified name of the class to be loaded; using '.' as separator.
     * @return a KSClassDeclaration, or null if not found.
     */
    fun getClassDeclarationByName(name: KSName): KSClassDeclaration?

    /**
     * Compose a type argument out of a type reference and a variance
     *
     * @param typeRef a type reference to be used in type argument
     * @param variance specifies a use-site variance
     * @return a type argument with use-site variance
     */
    fun getTypeArgument(typeRef: KSTypeReference, variance: Variance): KSTypeArgument

    /**
     * Get a [KSName] from a String.
     */
    fun getKSNameFromString(name: String): KSName

    /**
     * Create a [KSTypeReference] from a [KSType]
     */
    fun createKSTypeReferenceFromKSType(type: KSType): KSTypeReference

    /**
     * Provides built in types for convenience. For example, [KSBuiltins.anyType] is the KSType instance for class 'kotlin.Any'.
     */
    val builtIns: KSBuiltIns

    /**
     * map a declaration to jvm signature.
     */
    fun mapToJvmSignature(declaration: KSDeclaration): String

    /**
     * @param overrider the candidate overriding declaration being checked.
     * @param overridee the candidate overridden declaration being checked.
     * @return boolean value indicating whether [overrider] overrides [overridee]
     * Calling [overrides] is expensive and should be avoided if possible.
     */
    fun overrides(overrider: KSDeclaration, overridee: KSDeclaration): Boolean

    /**
     * Returns the type of [property] when it is viewed as member of [containing].
     *
     * For instance, for the following input:
     * ```
     * class Base<T>(val x:T) {
     * }
     * val foo: Base<Int>
     * val bar: Base<String>
     * ```
     * When `x` is viewed as member of `foo`, this method will return the [KSType] for `kotlin.Int` whereas when `x` is
     * viewed as member of `bar`, this method will return the KSType representing `kotlin.String`.
     *
     * If the given type does not contain [property], the [KSPropertyDeclaration.type type] of the [property] will be
     * returned.
     *
     * @param property The property whose type will be returned
     * @param containing The type that contains [property]
     */
    fun asMemberOf(
        property: KSPropertyDeclaration,
        containing: KSType
    ): KSType
}