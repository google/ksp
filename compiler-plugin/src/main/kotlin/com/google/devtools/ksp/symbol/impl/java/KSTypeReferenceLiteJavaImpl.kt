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

package com.google.devtools.ksp.symbol.impl.java

import com.google.devtools.ksp.ExceptionMessage
import com.google.devtools.ksp.processing.impl.KSObjectCache
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSReferenceElement
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitor
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.NonExistLocation
import com.google.devtools.ksp.symbol.Origin
import com.google.devtools.ksp.symbol.impl.kotlin.KSErrorType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod

class KSTypeReferenceLiteJavaImpl private constructor(val psiElement: PsiElement, override val parent: KSNode) :
    KSTypeReference {
    companion object : KSObjectCache<KSNode, KSTypeReferenceLiteJavaImpl>() {
        fun getCached(psiElement: PsiElement, parent: KSNode) = cache
            .getOrPut(parent) { KSTypeReferenceLiteJavaImpl(psiElement, parent) }
    }

    val type: KSType by lazy {
        when (psiElement) {
            is PsiAnnotation -> {
                val nameReferenceElement = psiElement.nameReferenceElement!!
                val psiClass = nameReferenceElement.resolve() as? PsiClass
                psiClass?.let {
                    (psiElement.containingFile as? PsiJavaFile)?.let {
                        ResolverImpl.instance!!.incrementalContext.recordLookup(it, psiClass.qualifiedName!!)
                    }
                    KSClassDeclarationJavaImpl.getCached(psiClass).asStarProjectedType()
                } ?: KSErrorType(nameReferenceElement.text)
            }
            is PsiMethod -> {
                KSClassDeclarationJavaImpl.getCached(psiElement.containingClass!!).asStarProjectedType()
            }
            else -> throw IllegalStateException(
                "Unexpected psi type in KSTypeReferenceLiteJavaImpl: ${psiElement.javaClass}, $ExceptionMessage"
            )
        }
    }

    override val origin = Origin.JAVA

    override val location: Location = NonExistLocation

    override val element: KSReferenceElement by lazy {
        KSClassifierReferenceLiteImplForJava.getCached(this)
    }

    override val annotations: Sequence<KSAnnotation> = emptySequence()

    override val modifiers: Set<Modifier> = emptySet()

    override fun resolve(): KSType {
        return type
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun toString(): String {
        return type.toString()
    }
}
