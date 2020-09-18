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
 *  Classes, functions, properties and typealiases can be declared as `expect` in common modules and `actual` in platform modules.
 *
 *  See https://kotlinlang.org/docs/reference/platform-specific-declarations.html for more information.
 */
interface KSExpectActual {
    /**
     * True if this is an `actual` implementation.
     */
    val isActual: Boolean

    /**
     * True if this is an `expect` declaration.
     */
    val isExpect: Boolean

    /**
     * Finds all corresponding `actual` implementations for `this`.
     *
     * @return a list of corresponding `actual` implementations, or an empty list if not applicable.
     */
    fun findActuals(): List<KSDeclaration>

    /**
     * Finds all corresponding `expect` declarations for `this`.
     *
     * @return a list of corresponding `expect` implementations, or an empty list if not applicable.
     */
    fun findExpects(): List<KSDeclaration>
}