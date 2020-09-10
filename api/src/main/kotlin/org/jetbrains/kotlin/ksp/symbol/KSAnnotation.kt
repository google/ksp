/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * Instance of a constructor-call-like annotation.
 */
interface KSAnnotation : KSNode {
    /**
     * Reference to type of the annotation class declaration.
     */
    val annotationType: KSTypeReference

    /**
     * The arguments applied to the constructor call to construct this annotation.
     * Must be compile time constants.
     * @see [KSValueArgument] for operations on its values.
     */
    val arguments: List<KSValueArgument>

    /**
     * Short name for this annotation, equivalent to the simple name of the declaration of the annotation class.
     */
    val shortName: KSName

    /**
     * Use site target of the annotation. Could be null if no annotation use site target is specified.
     */
    val useSiteTarget: AnnotationUseSiteTarget?
}
