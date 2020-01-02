/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * A declaration, can be function declaration, clsss declaration and property declaration, or a type alias.
 */
interface KSDeclaration : KSModifierListOwner, KSAnnotated {
    /**
     * Simple name of this declaration, usually the name identifier at the declaration site.
     */
    val simpleName: KSName

    /**
     * Fully qualified name of this declaration, might not exist for some declarations like local declarations.
     */
    val qualifiedName: KSName?

    /**
     * List of [type parameters][KSTypeParameter] of the declaration.
     */
    val typeParameters: List<KSTypeParameter>

    // TODO: support package
    /**
     * Parent declaration of this declaration, i.e. the declaration that directly contains this declaration.
     * File is not a declaration, so this property will be null for top level declarations.
     */
    val parentDeclaration: KSDeclaration?

    /**
     * The containing source file of this declaration, can be null if symbol does not come from a source file, i.e. from a class file.
     */
    val containingFile: KSFile?
}
