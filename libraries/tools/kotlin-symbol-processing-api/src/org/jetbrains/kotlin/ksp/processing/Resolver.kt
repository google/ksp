/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processing

import org.jetbrains.kotlin.ksp.symbol.*

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
     * @param name full qualified name of the class to be loaded; using '.' as separator.
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
     * Provides built in types for convenience. For example, [KSBuiltins.anyType] is the KSType instance for class 'kotlin.Any'.
     */
    val builtIns: KSBuiltIns
}