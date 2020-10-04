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

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.KSObjectCache
import com.google.devtools.ksp.symbol.impl.binary.KSClassDeclarationDescriptorImpl
import com.google.devtools.ksp.symbol.impl.binary.KSClassifierReferenceDescriptorImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSErrorType
import com.google.devtools.ksp.symbol.impl.toLocation
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance

class KSTypeReferenceJavaImpl private constructor(val psi: PsiType) : KSTypeReference {
    companion object : KSObjectCache<PsiType, KSTypeReferenceJavaImpl>() {
        fun getCached(psi: PsiType) = cache.getOrPut(psi) { KSTypeReferenceJavaImpl(psi) }
    }

    override val origin = Origin.JAVA

    override val location: Location by lazy {
        (psi as? PsiClassReferenceType)?.reference?.toLocation() ?: NonExistLocation
    }

    override val annotations: List<KSAnnotation> by lazy {
        psi.annotations.map { KSAnnotationJavaImpl.getCached(it) }
    }

    override val modifiers: Set<Modifier> = emptySet()

    override val element: KSReferenceElement by lazy {
        fun PsiPrimitiveType.toKotlinType(): KotlinType {
            return when (this.name) {
                "int" -> ResolverImpl.instance.module.builtIns.intType
                "short" -> ResolverImpl.instance.module.builtIns.shortType
                "byte" -> ResolverImpl.instance.module.builtIns.byteType
                "long" -> ResolverImpl.instance.module.builtIns.longType
                "float" -> ResolverImpl.instance.module.builtIns.floatType
                "double" -> ResolverImpl.instance.module.builtIns.doubleType
                "char" -> ResolverImpl.instance.module.builtIns.charType
                "boolean" -> ResolverImpl.instance.module.builtIns.booleanType
                "void" -> ResolverImpl.instance.module.builtIns.unitType
                else -> throw IllegalStateException()
            }
        }

        val type = if (psi is PsiWildcardType) {
            psi.bound
        } else {
            psi
        }
        when (type) {
            is PsiClassType -> KSClassifierReferenceJavaImpl.getCached(type)
            is PsiWildcardType -> KSClassifierReferenceJavaImpl.getCached(type.extendsBound as PsiClassType)
            is PsiPrimitiveType -> KSClassifierReferenceDescriptorImpl.getCached(type.toKotlinType())
            is PsiArrayType -> {
                val componentType = ResolverImpl.instance.resolveJavaType(type.componentType)
                if (type.componentType !is PsiPrimitiveType) {
                    KSClassifierReferenceDescriptorImpl.getCached(
                        ResolverImpl.instance.module.builtIns.getArrayType(Variance.INVARIANT, componentType)
                    )
                } else {
                    KSClassifierReferenceDescriptorImpl.getCached(
                        ResolverImpl.instance.module.builtIns.getPrimitiveArrayKotlinTypeByPrimitiveKotlinType(componentType)!!
                    )
                }
            }
            else -> throw IllegalStateException()
        }
    }

    override fun resolve(): KSType {
        val resolvedType = ResolverImpl.instance.resolveUserType(this)
        return if ((resolvedType.declaration as? KSClassDeclarationDescriptorImpl)?.descriptor is NotFoundClasses.MockClassDescriptor) {
            KSErrorType
        } else resolvedType
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitTypeReference(this, data)
    }

    override fun toString(): String {
        return element.toString()
    }
}