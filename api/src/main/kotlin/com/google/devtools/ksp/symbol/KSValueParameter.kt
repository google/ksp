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
 * A value parameter
 */
interface KSValueParameter : KSAnnotated {
    /**
     * Name of the parameter
     */
    val name: KSName?

    /**
     *  The reference to the type of the parameter.
     */
    val type: KSTypeReference

    /**
     * The containing source file of this declaration, can be null if symbol does not come from a source file, i.e. from a class file.
     */
    val containingFile: KSFile?

    /**
     * True if it is a vararg.
     */
    val isVararg: Boolean

    /**
     * True if it has the `noinline` modifier
     */
    val isNoInline: Boolean

    /**
     * True if it has the `crossinline` modifier
     */
    val isCrossInline: Boolean

    /**
     * True if it is a value
     */
    val isVal: Boolean

    /**
     * True if it is a variable
     */
    val isVar: Boolean

    /**
     * True if it has a default value
     */
    val hasDefault: Boolean
}
