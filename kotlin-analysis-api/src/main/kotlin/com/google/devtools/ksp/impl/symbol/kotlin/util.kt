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

import com.google.devtools.ksp.getDocString
import com.google.devtools.ksp.impl.KSPCoreEnvironment
import com.google.devtools.ksp.symbol.*
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtAnalysisSessionProvider
import org.jetbrains.kotlin.analysis.api.analyseWithCustomToken
import org.jetbrains.kotlin.analysis.api.annotations.annotations
import org.jetbrains.kotlin.analysis.api.symbols.KtEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtAnnotatedSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.tokens.AlwaysAccessibleValidityTokenFactory
import org.jetbrains.kotlin.psi.KtElement

internal val ktSymbolOriginToOrigin = mapOf(
    KtSymbolOrigin.JAVA to Origin.JAVA,
    KtSymbolOrigin.SOURCE to Origin.KOTLIN,
    KtSymbolOrigin.SAM_CONSTRUCTOR to Origin.SYNTHETIC,
    KtSymbolOrigin.SOURCE_MEMBER_GENERATED to Origin.SYNTHETIC,
    KtSymbolOrigin.DELEGATED to Origin.SYNTHETIC,
    KtSymbolOrigin.PROPERTY_BACKING_FIELD to Origin.KOTLIN,
    KtSymbolOrigin.JAVA_SYNTHETIC_PROPERTY to Origin.SYNTHETIC,
    KtSymbolOrigin.INTERSECTION_OVERRIDE to Origin.KOTLIN
)

internal fun mapAAOrigin(ktSymbolOrigin: KtSymbolOrigin): Origin {
    return ktSymbolOriginToOrigin[ktSymbolOrigin]
        ?: throw IllegalStateException("unhandled origin ${ktSymbolOrigin.name}")
}

internal fun PsiElement?.toLocation(): Location {
    if (this == null) {
        return NonExistLocation
    }
    val file = this.containingFile
    val document = KSPCoreEnvironment.instance.psiDocumentManager.getDocument(file) ?: return NonExistLocation
    return FileLocation(file.virtualFile.path, document.getLineNumber(this.textOffset) + 1)
}

internal fun KtSymbol.toContainingFile(): KSFile? {
    return when (val psi = this.psi) {
        is KtElement -> analyseWithCustomToken(psi, AlwaysAccessibleValidityTokenFactory) {
            KSFileImpl(psi.containingKtFile.getFileSymbol())
        }
        else -> null
    }
}

internal fun KtSymbol.toDocString(): String? = this.psi?.getDocString()

// TODO: migrate to analyzeWithKtModule once it's available.
@OptIn(InvalidWayOfUsingAnalysisSession::class)
internal inline fun <R> analyzeWithSymbolAsContext(
    contextSymbol: KtSymbol,
    action: KtAnalysisSession.() -> R
): R {
    return KtAnalysisSessionProvider.getInstance(KSPCoreEnvironment.instance.project)
        .analyzeWithSymbolAsContext(contextSymbol, action)
}

internal fun KtSymbolWithMembers.declarations(): Sequence<KSDeclaration> {
    return analyzeWithSymbolAsContext(this) {
        this@declarations.getDeclaredMemberScope().getAllSymbols().map {
            when (it) {
                is KtNamedClassOrObjectSymbol -> KSClassDeclarationImpl(it)
                is KtFunctionLikeSymbol -> KSFunctionDeclarationImpl(it)
                is KtPropertySymbol -> KSPropertyDeclarationImpl(it)
                is KtEnumEntrySymbol -> KSClassDeclarationEnumEntryImpl(it)
                else -> throw IllegalStateException()
            }
        }
    }
}

internal fun KtAnnotatedSymbol.annotations(): Sequence<KSAnnotation> {
    return this.annotations.asSequence().map { KSAnnotationImpl(it) }
}
