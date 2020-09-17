/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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
     * Provides built in types for convenience. For example, [KSBuiltins.anyType] is the KSType instance for class 'kotlin.Any'.
     */
    val builtIns: KSBuiltIns

    /**
     * map a declaration to jvm signature.
     */
    fun mapToJvmSignature(declaration: KSDeclaration): String
}