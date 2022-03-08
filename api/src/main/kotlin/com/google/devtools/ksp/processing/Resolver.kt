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

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.symbol.*

/**
 * [Resolver] provides [SymbolProcessor] with access to compiler details such as Symbols.
 */
interface Resolver {
    /**
     * Get all new files in the module / compilation unit.
     *
     * @return new files generated from last last round of processing in the module.
     */
    fun getNewFiles(): Sequence<KSFile>

    /**
     * Get all files in the module / compilation unit.
     *
     * @return all input files including generated files from previous rounds, note when incremental is enabled, only dirty files up for processing will be returned.
     */
    fun getAllFiles(): Sequence<KSFile>

    /**
     * Get all symbols with specified annotation.
     * Note that in multiple round processing, only symbols from deferred symbols of last round and symbols from newly generated files will be returned in this function.
     *
     * @param annotationName is the fully qualified name of the annotation; using '.' as separator.
     * @param inDepth whether to check symbols in depth, i.e. check symbols from local declarations. Operation can be expensive if true.
     * @return Elements annotated with the specified annotation.
     */
    fun getSymbolsWithAnnotation(annotationName: String, inDepth: Boolean = false): Sequence<KSAnnotated>

    /**
     * Find a class in the compilation classpath for the given name.
     *
     * This returns the exact platform class when given a platform name. Note that java.lang.String isn't compatible
     * with kotlin.String in the type system. Therefore, processors need to use mapJavaNameToKotlin() and mapKotlinNameToJava()
     * explicitly to find the corresponding class names before calling getClassDeclarationByName if type checking
     * is needed for the classes loaded by this.
     *
     * This behavior is limited to getClassDeclarationByName; When processors get a class or type from a Java source
     * file, the conversion is done automatically. E.g., a java.lang.String in a Java source file is loaded as
     * kotlin.String in KSP.
     *
     * @param name fully qualified name of the class to be loaded; using '.' as separator.
     * @return a KSClassDeclaration, or null if not found.
     */
    fun getClassDeclarationByName(name: KSName): KSClassDeclaration?

    /**
     * Find functions in the compilation classpath for the given name.
     *
     * @param name fully qualified name of the function to be loaded; using '.' as separator.
     * @param includeTopLevel a boolean value indicate if top level functions should be searched. Default false. Note if top level functions are included, this operation can be expensive.
     * @return a Sequence of KSFunctionDeclaration
     */
    fun getFunctionDeclarationsByName(name: KSName, includeTopLevel: Boolean = false): Sequence<KSFunctionDeclaration>

    /**
     * Find a property in the compilation classpath for the given name.
     *
     * @param name fully qualified name of the property to be loaded; using '.' as separator.
     * @param includeTopLevel a boolean value indicate if top level properties should be searched. Default false. Note if top level properties are included, this operation can be expensive.
     * @return a KSPropertyDeclaration, or null if not found.
     */
    fun getPropertyDeclarationByName(name: KSName, includeTopLevel: Boolean = false): KSPropertyDeclaration?

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
     * This function might fail due to resolution error, in case of error, null is returned.
     * Resolution error could be caused by bad code that could not be resolved by compiler, or KSP bugs.
     * If you believe your code is correct, please file a bug at https://github.com/google/ksp/issues/new
     */
    @KspExperimental
    fun mapToJvmSignature(declaration: KSDeclaration): String?

    /**
     * @param overrider the candidate overriding declaration being checked.
     * @param overridee the candidate overridden declaration being checked.
     * @return boolean value indicating whether [overrider] overrides [overridee]
     * Calling [overrides] is expensive and should be avoided if possible.
     */
    fun overrides(overrider: KSDeclaration, overridee: KSDeclaration): Boolean

    /**
     * @param overrider the candidate overriding declaration being checked.
     * @param overridee the candidate overridden declaration being checked.
     * @param containingClass the containing class of candidate overriding and overridden declaration being checked.
     * @return boolean value indicating whether [overrider] overrides [overridee]
     * Calling [overrides] is expensive and should be avoided if possible.
     */
    fun overrides(overrider: KSDeclaration, overridee: KSDeclaration, containingClass: KSClassDeclaration): Boolean

    /**
     * Returns the jvm name of the given function.
     * This function might fail due to resolution error, in case of error, null is returned.
     * Resolution error could be caused by bad code that could not be resolved by compiler, or KSP bugs.
     * If you believe your code is correct, please file a bug at https://github.com/google/ksp/issues/new
     *
     * The jvm name of a function might depend on the Kotlin Compiler version hence it is not guaranteed to be
     * compatible between different compiler versions except for the rules outlined in the Java interoperability
     * documentation: https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html.
     *
     * If the [declaration] is annotated with [JvmName], that name will be returned from this function.
     *
     * Note that this might be different from the name declared in the Kotlin source code in two cases:
     * a) If the function receives or returns an inline class, its name will be mangled according to
     * https://kotlinlang.org/docs/reference/inline-classes.html#mangling.
     * b) If the function is declared as internal, it will include a suffix with the module name.
     *
     * NOTE: As inline classes are an experimental feature, the result of this function might change based on the
     * kotlin version used in the project.
     */
    @KspExperimental
    fun getJvmName(declaration: KSFunctionDeclaration): String?

    /**
     * Returns the jvm name of the given property accessor.
     * This function might fail due to resolution error, in case of error, null is returned.
     * Resolution error could be caused by bad code that could not be resolved by compiler, or KSP bugs.
     * If you believe your code is correct, please file a bug at https://github.com/google/ksp/issues/new
     *
     * The jvm name of an accessor might depend on the Kotlin Compiler version hence it is not guaranteed to be
     * compatible between different compiler versions except for the rules outlined in the Java interoperability
     * documentation: https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html.
     *
     * If the [accessor] is annotated with [JvmName], that name will be returned from this function.
     *
     * By default, this name will match the name calculated according to
     * https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties.
     * Note that the result of this function might be different from that name in two cases:
     * a) If the property's type is an internal class, accessor's name will be mangled according to
     * https://kotlinlang.org/docs/reference/inline-classes.html#mangling.
     * b) If the function is declared as internal, it will include a suffix with the module name.
     *
     * NOTE: As inline classes are an experimental feature, the result of this function might change based on the
     * kotlin version used in the project.
     * see: https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html#properties
     */
    @KspExperimental
    fun getJvmName(accessor: KSPropertyAccessor): String?

    /**
     * Returns the [binary class name](https://asm.ow2.io/javadoc/org/objectweb/asm/Type.html#getClassName()) of the
     * owner class in JVM for the given [KSPropertyDeclaration].
     *
     * For properties declared in classes / interfaces; this value is the binary class name of the declaring class.
     *
     * For top level properties, this is the binary class name of the synthetic class that is generated for the Kotlin
     * file.
     * see: https://kotlinlang.org/docs/java-to-kotlin-interop.html#package-level-functions
     *
     * Note that, for properties declared in companion objects, the returned owner class will be the Companion class.
     * see: https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-methods
     */
    @KspExperimental
    fun getOwnerJvmClassName(declaration: KSPropertyDeclaration): String?

    /**
     * Returns the [binary class name](https://asm.ow2.io/javadoc/org/objectweb/asm/Type.html#getClassName()) of the
     * owner class in JVM for the given [KSFunctionDeclaration].
     *
     * For functions declared in classes / interfaces; this value is the binary class name of the declaring class.
     *
     * For top level functions, this is the binary class name of the synthetic class that is generated for the Kotlin
     * file.
     * see: https://kotlinlang.org/docs/java-to-kotlin-interop.html#package-level-functions
     *
     * Note that, for functions declared in companion objects, the returned owner class will be the Companion class.
     * see: https://kotlinlang.org/docs/java-to-kotlin-interop.html#static-methods
     */
    @KspExperimental
    fun getOwnerJvmClassName(declaration: KSFunctionDeclaration): String?

    /**
     * Returns checked exceptions declared in a function's header.
     * @return A sequence of [KSType] declared in `throws` statement for a Java method or in @Throws annotation for a Kotlin function.
     * Checked exceptions from class files are not supported yet, an empty sequence will be returned instead.
     */
    @KspExperimental
    fun getJvmCheckedException(function: KSFunctionDeclaration): Sequence<KSType>

    /**
     * Returns checked exceptions declared in a property accessor's header.
     * @return A sequence of [KSType] declared @Throws annotation for a Kotlin property accessor.
     * Checked exceptions from class files are not supported yet, an empty sequence will be returned instead.
     */
    @KspExperimental
    fun getJvmCheckedException(accessor: KSPropertyAccessor): Sequence<KSType>

    /**
     * Returns declarations with the given package name.
     * @param packageName the package name to look up.
     * @return A sequence of [KSDeclaration] with matching package name.
     * This will return declarations from both dependencies and source.
     */
    @KspExperimental
    fun getDeclarationsFromPackage(packageName: String): Sequence<KSDeclaration>

    /**
     * Returns the corresponding Kotlin class with the given Java class.
     *
     * E.g.
     * java.lang.String -> kotlin.String
     * java.lang.Integer -> kotlin.Int
     * java.util.List -> kotlin.List
     * java.util.Map.Entry -> kotlin.Map.Entry
     * java.lang.Void -> null
     *
     * @param javaName a Java class name
     * @return corresponding Kotlin class name or null
     */
    @KspExperimental
    fun mapJavaNameToKotlin(javaName: KSName): KSName?

    /**
     * Returns the corresponding Java class with the given Kotlin class.
     *
     * E.g.
     * kotlin.Throwable -> java.lang.Throwable
     * kotlin.Int -> java.lang.Integer
     * kotlin.Nothing -> java.lang.Void
     * kotlin.IntArray -> null
     *
     * @param kotlinName a Java class name
     * @return corresponding Java class name or null
     */
    @KspExperimental
    fun mapKotlinNameToJava(kotlinName: KSName): KSName?

    /**
     * Same as KSDeclarationContainer.declarations, but sorted by declaration order in the source.
     *
     * Note that this is SLOW. AVOID IF POSSIBLE.
     */
    @KspExperimental
    fun getDeclarationsInSourceOrder(container: KSDeclarationContainer): Sequence<KSDeclaration>

    /**
     * Returns a set of effective Java modifiers, if declaration is being / was generated to Java bytecode.
     */
    @KspExperimental
    fun effectiveJavaModifiers(declaration: KSDeclaration): Set<Modifier>

    /**
     * Compute the corresponding Java wildcard, from the given reference.
     *
     * @param reference the reference to the type usage
     * @return an equivalent type reference from the Java wildcard's point of view
     */
    @KspExperimental
    fun getJavaWildcard(reference: KSTypeReference): KSTypeReference

    /**
     * Tests a type if it was declared as legacy "raw" type in Java - a type with its type arguments fully omitted.
     *
     * @param type a type to check.
     * @return True if the type is a "raw" type.
     */
    @KspExperimental
    fun isJavaRawType(type: KSType): Boolean
}
