/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.google.devtools.ksp.impl

import com.google.devtools.ksp.impl.symbol.java.KSAnnotationJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSClassDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSFunctionDeclarationImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSPropertyDeclarationJavaImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeParameterImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSTypeReferenceImpl
import com.google.devtools.ksp.impl.symbol.kotlin.KSValueParameterImpl
import com.google.devtools.ksp.impl.symbol.kotlin.analyze
import com.google.devtools.ksp.impl.symbol.kotlin.classifierSymbol
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeArgumentResolvedImpl
import com.google.devtools.ksp.impl.symbol.kotlin.resolved.KSTypeReferenceResolvedImpl
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Origin
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.util.PsiTreeUtil
import java.io.File
import java.util.Collections
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Object that tracks error types queried through the KSP model and reports unrecovered errors in the final round.
 */
object KspDiagnostics {
    private val queriedErrors = Collections.synchronizedSet(LinkedHashSet<QueriedError>())

    fun recordQueriedError(typeRef: KSTypeReference, resolved: KSType) {
        when (typeRef) {
            is KSTypeReferenceImpl -> {
                queriedErrors.add(KotlinQueriedError(typeRef.ktTypeReference))
            }
            is KSTypeReferenceResolvedImpl -> {
                recordResolvedError(typeRef, resolved)
            }
            else -> {
                val typeName = extractTypeName(resolved)
                queriedErrors.add(GenericQueriedError(typeName, typeRef.location, typeRef.parent))
            }
        }
    }

    private fun recordResolvedError(typeRef: KSTypeReferenceResolvedImpl, resolved: KSType) {
        var effectiveParent = typeRef.parent
        while (effectiveParent is KSTypeArgumentResolvedImpl) {
            effectiveParent = effectiveParent.parent
        }

        if (typeRef.origin == Origin.JAVA || effectiveParent?.origin == Origin.JAVA) {
            val psiRefs = findJavaReferenceElements(effectiveParent, typeRef.index)
            if (psiRefs.isNotEmpty()) {
                val unresolved = psiRefs.filter { it.resolve() == null }
                if (unresolved.isNotEmpty()) {
                    unresolved.forEach { queriedErrors.add(JavaQueriedError(it)) }
                    return
                }
            }
        }

        if (typeRef.origin == Origin.KOTLIN || effectiveParent?.origin == Origin.KOTLIN) {
            val ktRef = findKotlinTypeReference(effectiveParent, typeRef.index)
            if (ktRef != null) {
                queriedErrors.add(KotlinQueriedError(ktRef))
                return
            }
        }

        val typeName = extractTypeName(resolved)
        val loc = if (typeRef.location !is FileLocation && effectiveParent != null) {
            effectiveParent.location
        } else {
            typeRef.location
        }
        queriedErrors.add(GenericQueriedError(typeName, loc, effectiveParent ?: typeRef.parent))
    }

    private fun findJavaReferenceElements(parent: KSNode?, index: Int): List<PsiJavaCodeReferenceElement> {
        if (parent is KSAnnotationJavaImpl) {
            val nameRef = parent.psi.nameReferenceElement
            if (nameRef != null) {
                return listOf(nameRef)
            }
        }

        val typeElement = when (parent) {
            is KSPropertyDeclarationJavaImpl -> {
                (parent.ktDeclarationSymbol.psi as? PsiField)?.typeElement
            }
            is KSFunctionDeclarationImpl -> {
                (parent.ktDeclarationSymbol.psi as? PsiMethod)?.returnTypeElement
            }
            is KSValueParameterImpl -> {
                (parent.ktValueParameterSymbol.psi as? PsiParameter)?.typeElement
            }
            else -> null
        }
        if (typeElement != null) {
            return PsiTreeUtil.findChildrenOfType(typeElement, PsiJavaCodeReferenceElement::class.java).toList()
        }

        return when (parent) {
            is KSClassDeclarationImpl -> {
                val psiClass = parent.ktDeclarationSymbol.psi as? PsiClass
                val allRefs = (psiClass?.extendsList?.referenceElements?.toList() ?: emptyList()) +
                    (psiClass?.implementsList?.referenceElements?.toList() ?: emptyList())
                if (index in allRefs.indices) listOf(allRefs[index]) else allRefs
            }
            is KSTypeParameterImpl -> {
                val psiParam = parent.ktDeclarationSymbol.psi as? PsiTypeParameter
                val allRefs = psiParam?.extendsList?.referenceElements?.toList() ?: emptyList()
                if (index in allRefs.indices) listOf(allRefs[index]) else allRefs
            }
            else -> emptyList()
        }
    }

    private fun findKotlinTypeReference(parent: KSNode?, index: Int): KtTypeReference? {
        return when (parent) {
            is KSClassDeclarationImpl -> {
                val ktClass = parent.ktClassOrObjectSymbol.psi as? KtClassOrObject
                ktClass?.superTypeListEntries?.getOrNull(index)?.typeReference
            }
            is KSTypeParameterImpl -> {
                val ktParam = parent.ktTypeParameterSymbol.psi as? KtTypeParameter
                ktParam?.extendsBound
            }
            else -> null
        }
    }

    @OptIn(KaNonPublicApi::class)
    private fun extractTypeName(type: KSType): String? {
        val raw = if (type is KSTypeImpl) {
            val kaType = type.type
            if (kaType is KaClassErrorType) {
                kaType.qualifiers.joinToString(".") { it.name.asString() }
            } else if (kaType is KaErrorType) {
                kaType.presentableText
            } else {
                null
            }
        } else {
            null
        } ?: run {
            val str = type.toString()
            if (str.startsWith("<ERROR TYPE: ") && str.endsWith(">")) {
                str.removeSurrounding("<ERROR TYPE: ", ">").substringBefore(" % ")
            } else {
                str.takeIf { it != "<ERROR TYPE>" }
            }
        }
        return raw?.removePrefix("__KSP_unresolved_")
    }

    fun reportUnresolvedErrors(logger: KSPLogger) {
        val reportedMessages = mutableSetOf<String>()
        val delegatingLogger = object : KSPLogger by logger {
            override fun error(message: String, symbol: KSNode?) {
                if (reportedMessages.add(message)) {
                    logger.error(message, symbol)
                }
            }
        }
        val stillFailing = queriedErrors.filter { it.isStillError() }
        for (error in stillFailing) {
            error.report(delegatingLogger)
        }
    }

    fun clear() {
        queriedErrors.clear()
    }
}

private sealed interface QueriedError {
    fun isStillError(): Boolean
    fun report(logger: KSPLogger)
}

private class KotlinQueriedError(val ktTypeReference: KtTypeReference) : QueriedError {
    override fun isStillError(): Boolean {
        return try {
            analyze {
                val type = ktTypeReference.type
                type is KaErrorType || type.classifierSymbol() == null
            }
        } catch (_: Exception) {
            true
        }
    }

    override fun report(logger: KSPLogger) {
        val diagnosticMessage = try {
            analyze {
                val file = ktTypeReference.containingKtFile
                val diagnostics = file.collectDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
                val matchingDiagnostic = diagnostics.firstOrNull { diag ->
                    diag.severity == KaSeverity.ERROR && PsiTreeUtil.isAncestor(ktTypeReference, diag.psi, false)
                }
                matchingDiagnostic?.let { it.psi.withLocation(it.defaultMessage) }
            }
        } catch (_: Exception) {
            null
        }
        val message = diagnosticMessage ?: ktTypeReference.withLocation("Unresolved reference '${ktTypeReference.text}'")
        logger.error(message, null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is KotlinQueriedError) return false
        return ktTypeReference == other.ktTypeReference
    }

    override fun hashCode(): Int = ktTypeReference.hashCode()
}

private class JavaQueriedError(val reference: PsiJavaCodeReferenceElement) : QueriedError {
    override fun isStillError(): Boolean {
        return try {
            reference.resolve() == null
        } catch (_: Exception) {
            true
        }
    }

    override fun report(logger: KSPLogger) {
        val targetElement = reference.referenceNameElement ?: reference
        val name = reference.referenceName ?: reference.text
        logger.error(targetElement.withLocation("Unresolved reference '$name'"), null)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JavaQueriedError) return false
        return reference == other.reference
    }

    override fun hashCode(): Int = reference.hashCode()
}

private data class GenericQueriedError(
    val typeName: String?,
    val location: Location,
    val parentNode: KSNode?
) : QueriedError {
    override fun isStillError(): Boolean {
        if (typeName == null) return true
        return try {
            ResolverAAImpl.instance.getClassDeclarationByName(
                ResolverAAImpl.instance.getKSNameFromString(typeName)
            ) == null
        } catch (_: Exception) {
            true
        }
    }

    override fun report(logger: KSPLogger) {
        val message = "Unresolved reference '${typeName ?: "unknown"}'"
        when (location) {
            is FileLocation -> {
                val fileName = File(location.filePath).name
                logger.error("$fileName:${location.lineNumber}: $message", null)
            }
            else -> logger.error(message, parentNode)
        }
    }
}

private fun PsiElement.withLocation(message: String): String {
    val fileLoc = containingFile?.name
    val line = containingFile?.viewProvider?.document?.getLineNumber(textOffset)?.plus(1)
    val prefix = if (fileLoc != null && line != null) {
        "$fileLoc:$line: "
    } else if (fileLoc != null) {
        "$fileLoc: "
    } else {
        ""
    }

    return "$prefix$message"
}
