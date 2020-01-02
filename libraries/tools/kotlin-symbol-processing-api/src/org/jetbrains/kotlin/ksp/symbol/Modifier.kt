/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * All possible modifiers presented in the Kotlin grammar.
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
    EXPECT, ACTUAL
}
