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
 * Kotlin Symbol Processing's representation of names. Can be simple or qualified names.
 */
interface KSName {
    /**
     * String representation of the name.
     */
    fun asString(): String

    /**
     * Qualifier of the name.
     */
    fun getQualifier(): String

    /**
     * If a qualified name, it is the last part. Otherwise it is the same as [asString]
     */
    fun getShortName(): String
}