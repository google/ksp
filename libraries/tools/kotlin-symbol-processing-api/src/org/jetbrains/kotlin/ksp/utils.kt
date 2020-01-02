/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp

import org.jetbrains.kotlin.ksp.symbol.*

/**
 * Get functions directly declared inside the class declaration.
 *
 * What are included: member functions, extension functions declared inside it, etc.
 * What are NOT included: inherited functions, extension functions declared outside it.
 */
fun KSClassDeclaration.getDeclaredFunctions(): List<KSFunctionDeclaration> {
    return this.declarations.filterIsInstance<KSFunctionDeclaration>()
}

/**
 * Get properties directly declared inside the class declaration.
 *
 * What are included: member properties, extension properties declared inside it, etc.
 * What are NOT included: inherited properties, extension properties declared outside it.
 */
fun KSClassDeclaration.getDeclaredProperties(): List<KSPropertyDeclaration> {
    return this.declarations.filterIsInstance<KSPropertyDeclaration>()
}

/**
 * Check whether this is a local declaration, or namely, declared in a function.
 */
fun KSDeclaration.isLocal(): Boolean {
    return this.parentDeclaration != null && this.parentDeclaration !is KSClassDeclaration
}

/**
 * Find the KSClassDeclaration that the alias points to recursively.
 */
fun KSTypeAlias.findActualType(): KSClassDeclaration {
    val resolvedType = this.type.resolve()?.declaration
    return if (resolvedType is KSTypeAlias) {
        resolvedType.findActualType()
    } else {
        resolvedType as KSClassDeclaration
    }
}

/**
 * Determine [Visibility] of a [KSDeclaration].
 */
fun KSDeclaration.getVisibility(): Visibility {
    return when {
        this.modifiers.contains(Modifier.PRIVATE) -> Visibility.PRIVATE
        this.modifiers.contains(Modifier.PROTECTED) -> Visibility.PROTECTED
        this.modifiers.contains(Modifier.INTERNAL) -> Visibility.INTERNAL
        this.isLocal() -> Visibility.LOCAL
        else -> Visibility.PUBLIC
    }
}

/**
 * get all super types for a class declaration
 * Calling [getAllSuperTypes] requires type resolution therefore is expensive and should be avoided if possible.
 */
fun KSClassDeclaration.getAllSuperTypes(): Set<KSType> {

    fun KSTypeParameter.getTypesUpperBound(): List<KSClassDeclaration> =
        this.bounds.flatMap {
            when (val resolvedDeclaration = it.resolve()?.declaration) {
                is KSClassDeclaration -> listOf(resolvedDeclaration)
                is KSTypeAlias -> listOf(resolvedDeclaration.findActualType())
                is KSTypeParameter -> resolvedDeclaration.getTypesUpperBound()
                else -> throw IllegalStateException()
            }
        }

    val allSuperTypes = mutableSetOf<KSType>()
    allSuperTypes.addAll(this.superTypes.mapNotNull { it.resolve() })

    allSuperTypes.addAll(
        this.superTypes
            .mapNotNull { it.resolve()?.declaration }
            .flatMap {
                when (it) {
                    is KSClassDeclaration -> it.getAllSuperTypes()
                    is KSTypeAlias -> it.findActualType().getAllSuperTypes()
                    is KSTypeParameter -> it.getTypesUpperBound().flatMap { it.getAllSuperTypes() }
                    else -> throw IllegalStateException()
                }
            }
    )

    return allSuperTypes
}

fun KSDeclaration.isOpen() = !this.isLocal()
        && (this.modifiers.contains(Modifier.OVERRIDE)
        || this.modifiers.contains(Modifier.ABSTRACT)
        || this.modifiers.contains(Modifier.OPEN)
        || (this.parentDeclaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE
        )

fun KSDeclaration.isPublic() = !this.modifiers.contains(Modifier.PRIVATE)
        && !this.modifiers.contains(Modifier.PROTECTED)
        && !this.modifiers.contains(Modifier.INTERNAL)

fun KSDeclaration.isInternal() = this.modifiers.contains(Modifier.INTERNAL)

fun KSDeclaration.isPrivate() = this.modifiers.contains(Modifier.PRIVATE)

// TODO: cross module visibility is not handled
fun KSDeclaration.isVisibleFrom(other: KSDeclaration): Boolean {
    return when {
        // locals are limited to lexical scope
        this.isLocal() -> this.parentDeclaration == other
        // file visibility or member
        this.isPrivate() -> {
            this.parentDeclaration == other.parentDeclaration
                    || this.parentDeclaration == other
                    || (
                    this.parentDeclaration == null
                            && other.parentDeclaration == null
                            && this.containingFile == other.containingFile
                    )
        }
        this.isPublic() -> true
        this.isInternal() && other.containingFile != null && this.containingFile != null -> true
        else -> false
    }

}
