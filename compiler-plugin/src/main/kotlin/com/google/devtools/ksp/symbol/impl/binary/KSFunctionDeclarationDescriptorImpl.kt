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


package com.google.devtools.ksp.symbol.impl.binary

import com.google.devtools.ksp.ExceptionMessage
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import com.google.devtools.ksp.isOpen
import com.google.devtools.ksp.isVisibleFrom
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.*
import com.google.devtools.ksp.symbol.impl.findClosestOverridee
import com.google.devtools.ksp.symbol.impl.java.KSFunctionDeclarationJavaImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSBlockExpressionImpl
import com.google.devtools.ksp.symbol.impl.kotlin.KSFunctionDeclarationImpl
import com.google.devtools.ksp.symbol.impl.toKSFunctionDeclaration
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiClassImplUtil
import com.intellij.psi.search.PsiSearchScopeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.util.PsiNavigateUtil
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.load.java.isFromJava
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.load.kotlin.toSourceElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.stubs.impl.Utils
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.source.getPsi
import kotlin.reflect.jvm.internal.impl.types.checker.UtilsKt

class KSFunctionDeclarationDescriptorImpl private constructor(val descriptor: FunctionDescriptor) : KSFunctionDeclaration,
    KSDeclarationDescriptorImpl(descriptor),
    KSExpectActual by KSExpectActualDescriptorImpl(descriptor) {
    companion object : KSObjectCache<FunctionDescriptor, KSFunctionDeclarationDescriptorImpl>() {
        fun getCached(descriptor: FunctionDescriptor) = cache.getOrPut(descriptor) { KSFunctionDeclarationDescriptorImpl(descriptor) }
    }

    override fun findOverridee(): KSFunctionDeclaration? {
        return descriptor.findClosestOverridee()?.toKSFunctionDeclaration()
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        descriptor.typeParameters.map { KSTypeParameterDescriptorImpl.getCached(it) }
    }

    override val declarations: List<KSDeclaration> = emptyList()

    override val extensionReceiver: KSTypeReference? by lazy {
        val extensionReceiver = descriptor.extensionReceiverParameter?.type
        if (extensionReceiver != null) {
            KSTypeReferenceDescriptorImpl.getCached(extensionReceiver)
        } else {
            null
        }
    }

    override val functionKind: FunctionKind by lazy {
        when {
            descriptor.dispatchReceiverParameter == null -> if (descriptor.isFromJava) FunctionKind.STATIC else FunctionKind.TOP_LEVEL
            !descriptor.name.isSpecial && descriptor.name.asString().isNotEmpty() -> FunctionKind.MEMBER
            descriptor is AnonymousFunctionDescriptor -> FunctionKind.ANONYMOUS
            else -> throw IllegalStateException("Unable to resolve FunctionKind for ${descriptor.fqNameSafe}, $ExceptionMessage")
        }
    }

    override val isAbstract: Boolean by lazy {
        this.modifiers.contains(Modifier.ABSTRACT)
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addAll(descriptor.toKSModifiers())
        modifiers.addAll(descriptor.toFunctionKSModifiers())
        modifiers
    }

    override val parameters: List<KSValueParameter> by lazy {
        descriptor.valueParameters.map { KSValueParameterDescriptorImpl.getCached(it) }
    }

    override val returnType: KSTypeReference? by lazy {
        val returnType = descriptor.returnType
        if (returnType == null) {
            null
        } else {
            KSTypeReferenceDescriptorImpl.getCached(returnType)
        }
    }

    override val body: KSExpression? by lazy {
        // FIXME: The expression in the descriptor cannot be resolved at this time. The corresponding resolve API has not been found yet
        (descriptor.findPsi() as? KtFunction)?.bodyExpression?.toKSExpression()
    }

    override val text: String by lazy {
        TODO("Not yet implemented")
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFunctionDeclaration(this, data)
    }
}