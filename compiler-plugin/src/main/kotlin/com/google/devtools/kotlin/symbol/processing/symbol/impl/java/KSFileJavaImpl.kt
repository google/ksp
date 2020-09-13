/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl.java

import com.intellij.psi.PsiJavaFile
import com.google.devtools.kotlin.symbol.processing.symbol.*
import com.google.devtools.kotlin.symbol.processing.symbol.impl.KSObjectCache
import com.google.devtools.kotlin.symbol.processing.symbol.impl.kotlin.KSNameImpl
import com.google.devtools.kotlin.symbol.processing.symbol.impl.toLocation

class KSFileJavaImpl private constructor(val psi: PsiJavaFile) : KSFile {
    companion object : KSObjectCache<PsiJavaFile, KSFileJavaImpl>() {
        fun getCached(psi: PsiJavaFile) = cache.getOrPut(psi) { KSFileJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val location: Location by lazy {
        psi.toLocation()
    }

    override val annotations: List<KSAnnotation> = emptyList()

    override val declarations: List<KSDeclaration> by lazy {
        psi.classes.map { KSClassDeclarationJavaImpl.getCached(it) }
    }

    override val fileName: String by lazy {
        psi.name
    }

    override val packageName: KSName = KSNameImpl.getCached(if (psi.packageName == "") "<root>" else psi.packageName)

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFile(this, data)
    }

    override fun toString(): String {
        return "File: ${this.fileName}"
    }
}