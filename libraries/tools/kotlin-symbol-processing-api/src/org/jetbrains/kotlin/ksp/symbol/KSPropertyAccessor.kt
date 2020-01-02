/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

// TODO: add owner property.
/**
 * The common base of property getter and setter.
 */
interface KSPropertyAccessor : KSAnnotated, KSModifierListOwner {
}

// TODO: Should these extend KSFunctionDeclaration?
/**
 * A property setter
 */
interface KSPropertySetter : KSPropertyAccessor {
    val parameter: KSVariableParameter
}

/**
 * A property getter
 */
interface KSPropertyGetter : KSPropertyAccessor {
    val returnType: KSTypeReference?
}