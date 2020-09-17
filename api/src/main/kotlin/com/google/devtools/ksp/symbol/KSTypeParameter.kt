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


package com.google.devtools.ksp.symbol

/**
 * A type parameter
 */
interface KSTypeParameter : KSDeclaration {
    /**
     * Name of the type parameter
     *
     * For example, in `class Foo<T>`, the name value is "T"
     */
    val name: KSName

    /**
     * Declaration-site variance
     */
    val variance: Variance

    /**
     * True if it is reified, i.e., has the reified modifier.
     */
    val isReified: Boolean

    /**
     * Upper bounds of the type parameter.
     */
    val bounds: List<KSTypeReference>
}