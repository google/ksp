/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * All possible modifiers presented in the Kotlin grammar.
 * Modifiers you can get from a declaration are explict modifiers as they are declared in source code.
 * Same modifier can be semantically different in different languages, therefore you should only rely on modifiers if you have a good
 * understanding of what it means in specific cases, otherwise you should rely on helper functions like isOpen() for modifier related logic.
 * Modifiers prefixed with "JAVA_" are java only modifiers.
 */
enum class Modifier {
    PUBLIC, PRIVATE, INTERNAL, PROTECTED,
    IN, OUT,
    OVERRIDE, LATEINIT,
    ENUM, SEALED, ANNOTATION, DATA, INNER,
    SUSPEND, TAILREC, OPERATOR, INFIX, INLINE, EXTERNAL,
    ABSTRACT, FINAL, OPEN,
    VARARG, NOINLINE, CROSSINLINE,
    REIFIED,
    EXPECT, ACTUAL,
    JAVA_DEFAULT, JAVA_NATIVE, JAVA_STATIC, JAVA_STRICT, JAVA_SYNCHRONIZED, JAVA_TRANSIENT, JAVA_VOLATILE
}
