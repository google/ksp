/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.java

import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.symbol.impl.kotlin.KSNameImpl

class KSFileJavaImpl(val psi: PsiJavaFile) : KSFile {
    companion object {
        private val cache = mutableMapOf<PsiJavaFile, KSFileJavaImpl>()

        fun getCached(psi: PsiJavaFile) = cache.getOrPut(psi) { KSFileJavaImpl(psi) }
    }

    override val annotations: List<KSAnnotation> = emptyList()

    override val declarations: List<KSDeclaration> by lazy {
        psi.classes.map { KSClassDeclarationJavaImpl.getCached(it) }
    }

    override val fileName: String by lazy {
        psi.name
    }

    override val packageName: KSName = KSNameImpl.getCached(psi.packageName)

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFile(this, data)
    }
}