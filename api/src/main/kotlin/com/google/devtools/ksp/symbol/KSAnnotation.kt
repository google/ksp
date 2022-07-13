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
 * Instance of a constructor-call-like annotation.
 */
interface KSAnnotation : KSNode {
    /**
     * Reference to the type of the annotation class declaration.
     */
    val annotationType: KSTypeReference

    /**
     * The arguments applied to the constructor call to construct this annotation.
     * Must be compile time constants.
     * @see [KSValueArgument] for operations on its values.
     */
    val arguments: List<KSValueArgument>

    /**
     * The default values of the annotation members
     */
    val defaultArguments: List<KSValueArgument>

    /**
     * Short name for this annotation, equivalent to the simple name of the declaration of the annotation class.
     */
    val shortName: KSName

    /**
     * Use site target of the annotation. Could be null if no annotation use site target is specified.
     */
    val useSiteTarget: AnnotationUseSiteTarget?

    /**
     * Returns the fully qualified name of the annotation, if available
     */
    fun resolveQualifiedName(): KSName?
}
