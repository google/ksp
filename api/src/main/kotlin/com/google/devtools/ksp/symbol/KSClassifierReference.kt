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
 * An application / reference to a user declared type such as class, interface and object.
 */
interface KSClassifierReference : KSReferenceElement {
    /**
     * The outer class of an inner class.
     */
    val qualifier: KSClassifierReference?

    /**
     * The text which appears in the reference. For example, it is "Int" in `val temperature: Int` or
     * "kotlin.Any" in `val canBeAnything: kotlin.Any`
     */
    fun referencedName(): String

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitClassifierReference(this, data)
    }
}
