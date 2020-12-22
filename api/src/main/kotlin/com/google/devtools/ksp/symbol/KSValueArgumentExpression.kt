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
 * A value argument expression to function / constructor calls.
 */
interface KSValueArgumentExpression : KSNode {
    /**
     * The name for the named argument, or null otherwise.
     *
     * For example, in `ignore(name=123456)`, the name value is "name"
     */
    val name: String?

    /**
     * The value of the argument.
     */
    val value: KSExpression?

    /**
     * The index of the argument in the list.
     *
     * @see KSCallExpression.arguments
     */
    val index: Int
}