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
 * Holds the information for a [KSFunctionDeclaration] where type arguments are resolved as member
 * of a specific [KSType].
 *
 * @see Resolver.asMemberOf
 */
interface KSFunction {
    /**
     * The return type of the function. Note that this might be `null` if an error happened when
     * the type is resolved.
     *
     * @see KSFunctionDeclaration.returnType
     */
    val returnType: KSType?

    /**
     * The types of the value parameters of the function. Note that this list might have `null`
     * values in it if the type of a parameter could not be resolved.
     *
     * @see KSFunctionDeclaration.parameters
     */
    val parameterTypes: List<KSType?>

    /**
     * The type parameters of the function.
     *
     * @see KSFunctionDeclaration.typeParameters
     */
    val typeParameters: List<KSTypeParameter>

    /**
     * The receiver type of the function.
     *
     * @see KSFunctionDeclaration.extensionReceiver
     */
    val extensionReceiverType: KSType?

    /**
     * True if the compiler couldn't resolve the function.
     */
    val isError: Boolean
}
