/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.ksp.symbol

/**
 * A symbol that can be annotated with annotations.
 */
interface KSAnnotated : KSNode {

    /**
     * All annotations on this symbol.
     */
    val annotations: List<KSAnnotation>
}