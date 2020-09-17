/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o and and Kotlin Programming Language contributors.
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


package com.google.devtools.ksp.symbol.impl.java

import com.intellij.psi.PsiJavaFile
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import com.google.devtools.ksp.symbol.impl.toLocation

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