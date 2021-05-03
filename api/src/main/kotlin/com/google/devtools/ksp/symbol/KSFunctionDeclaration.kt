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
import com.google.devtools.ksp.processing.Resolver
/**
 * A function definition
 *
 * Dispatch receiver can be obtained through [parentDeclaration].
 *
 * To obtain the function signature where type arguments are resolved as member of a given [KSType],
 * use [Resolver.asMemberOf].
 *
 * @see KSFunctionType
 */
interface KSFunctionDeclaration : KSDeclaration, KSDeclarationContainer {
    /**
     * Kind of this function.
     */
    val functionKind: FunctionKind

    /**
     * Whether this function is abstract.
     */
    val isAbstract: Boolean

    /**
     * Extension receiver of this function
     * @see [https://kotlinlang.org/docs/reference/extensions.html#extension-functions]
     */
    val extensionReceiver: KSTypeReference?

    /**
     * Return type of this function.
     * Can be null if an error occurred during resolution.
     */
    val returnType: KSTypeReference?

    /**
     * [value parameters][KSValueParameter] of this function.
     */
    val parameters: List<KSValueParameter>

    /**
     * Find the closest overridee of this function, if overriding.
     *
     * For the following input:
     * ```
     * abstract class A {
     *   open fun x() {}
     *   open fun y() {}
     * }
     * abstract class B : A() {
     *   override open fun x() {}
     * }
     * abstract class C : B() {
     *   override open fun x() {}
     *   override open fun y() {}
     * }
     * ```
     * Calling `findOverridee` on `C.x` will return `B.x`.
     * Calling `findOverridee` on `C.y` will return `A.y`.
     *
     * When there are multiple super interfaces implementing the function, the closest declaration
     * to the current containing declaration is selected. If they are in the same level, the
     * function of the first specified interface (in source) will be returned.
     *
     * @return [KSFunctionDeclaration] for the original function, if overriding, otherwise null.
     * Calling [findOverridee] is expensive and should be avoided if possible.
     */
    fun findOverridee(): KSFunctionDeclaration?

    /**
     * See Resolver.asMemberOf(function: KSFunctionDeclaration, containing: KSType).
     */
    fun asMemberOf(containing: KSType): KSFunction
}
