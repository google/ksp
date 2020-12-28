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
 * An expression that represents the calling property or function or constructor.
 *
 * @author RinOrz
 */
interface KSCallExpression : KSExpression {

    /**
     * The receiver expression for the call.
     * 
     * For example, in `boolean.toString().replace("t", "a")`, the receiver is "boolean.toString()"
     */
    val receiver: KSExpression?

    /**
     * The name for the calling target.
     */
    val name: String

    /**
     * The actual parameters of the call.
     * When the target of the call is a property, the arguments is null.
     */
    val arguments: List<KSValueArgumentExpression>?

    /**
     * Try resolves to the call target declaration site.
     *
     * @return A declaration resolved from this expression.
     * Calling [resolve] is expensive and should be avoided if possible.
     *
     * @see KSPropertyDeclaration When the target of the call is a property.
     * @see KSFunctionDeclaration When the target of the call is a function.
     */
    fun resolve(): KSDeclaration?
}