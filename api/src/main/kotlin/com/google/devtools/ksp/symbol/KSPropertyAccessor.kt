/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol

/**
 * The common base of property getter and setter.
 */
interface KSPropertyAccessor : KSAnnotated, KSModifierListOwner {
    /**
     * The owner of the property accessor.
     */
    val receiver: KSPropertyDeclaration
}

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