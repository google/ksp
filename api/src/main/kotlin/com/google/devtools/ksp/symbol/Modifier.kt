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
