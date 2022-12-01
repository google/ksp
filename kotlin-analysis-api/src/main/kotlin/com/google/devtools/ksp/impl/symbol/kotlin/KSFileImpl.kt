/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp.impl.symbol.kotlin

import com.google.devtools.ksp.KSObjectCache
import com.google.devtools.ksp.processing.impl.KSNameImpl
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSName
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.psi.KtFile

class KSFileImpl private constructor(private val ktFileSymbol: KtFileSymbol) : KSFile {
    companion object : KSObjectCache<KtFileSymbol, KSFileImpl>() {
        fun getCached(ktFileSymbol: KtFileSymbol) = cache.getOrPut(ktFileSymbol) { KSFileImpl(ktFileSymbol) }
    }

    private val psi: PsiFile
        get() = ktFileSymbol.psi as PsiFile

    override val packageName: KSName by lazy {
        when (psi) {
            is KtFile -> KSNameImpl.getCached((psi as KtFile).packageFqName.asString())
            is PsiJavaFile -> KSNameImpl.getCached((psi as PsiJavaFile).packageName)
            else -> throw IllegalStateException("Unhandled psi file type ${psi.javaClass}")
        }
    }

    override val fileName: String by lazy {
        psi.name
    }

    override val filePath: String by lazy {
        psi.virtualFile.path
    }

    override val declarations: Sequence<KSDeclaration> by lazy {
        analyze {
            ktFileSymbol.getFileScope().getAllSymbols().map {
                when (it) {
                    is KtNamedClassOrObjectSymbol -> KSClassDeclarationImpl.getCached(it)
                    is KtFunctionSymbol -> KSFunctionDeclarationImpl.getCached(it)
                    is KtPropertySymbol -> KSPropertyDeclarationImpl.getCached(it)
                    is KtTypeAliasSymbol -> KSTypeAliasImpl.getCached(it)
                    else -> throw IllegalStateException("Unhandled ${it.javaClass}")
                }
            }
        }
    }

    override val origin: Origin = Origin.KOTLIN

    override val location: Location by lazy {
        ktFileSymbol.psi.toLocation()
    }

    override val parent: KSNode? = null

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFile(this, data)
    }

    override val annotations: Sequence<KSAnnotation> by lazy {
        ktFileSymbol.annotations()
    }

    override fun toString(): String {
        return "File: ${this.fileName}"
    }
}
