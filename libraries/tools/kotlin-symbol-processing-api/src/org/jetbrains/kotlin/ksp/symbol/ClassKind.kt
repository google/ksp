/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol

/**
 * Kind of a class declaration.
 * Interface, class, enum class and object are all considered a class declaration.
 */
enum class ClassKind(val type: String) {
    INTERFACE("interface"),
    CLASS("class"),
    ENUM_CLASS("enum_class"),
    ENUM_ENTRY("enum_entry"),
    OBJECT("object"),
    ANNOTATION_CLASS("annotation_class")
}