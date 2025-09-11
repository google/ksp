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
import com.google.devtools.ksp.processing.impl.KSObjectCache
import com.google.devtools.ksp.processing.impl.ResolverImpl
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.load.java.isFromJava
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.org.objectweb.asm.Opcodes

class KSFunctionDeclarationDescriptorImpl private constructor(val descriptor: FunctionDescriptor) :
    KSFunctionDeclaration,
    KSDeclarationDescriptorImpl(descriptor),
    KSExpectActual by KSExpectActualDescriptorImpl(descriptor) {
    companion object : KSObjectCache<FunctionDescriptor, KSFunctionDeclarationDescriptorImpl>() {
        fun getCached(descriptor: FunctionDescriptor) =
            cache.getOrPut(descriptor) { KSFunctionDeclarationDescriptorImpl(descriptor) }
    }

    override fun asKSDeclaration(): KSDeclaration = this

    override fun findOverridee(): KSDeclaration? {
        return descriptor.findClosestOverridee()?.toKSDeclaration()
    }

    override val typeParameters: List<KSTypeParameter> by lazy {
        descriptor.typeParameters.map { KSTypeParameterDescriptorImpl.getCached(it) }
    }

    override val declarations: Sequence<KSDeclaration> = emptySequence()

    override val extensionReceiver: KSTypeReference? by lazy {
        val extensionReceiver = descriptor.extensionReceiverParameter?.type
        if (extensionReceiver != null) {
            KSTypeReferenceDescriptorImpl.getCached(extensionReceiver, origin, this)
        } else {
            null
        }
    }

    override val functionKind: FunctionKind by lazy {

        when {
            descriptor.dispatchReceiverParameter == null -> when {
                descriptor.isFromJava -> FunctionKind.STATIC
                else -> FunctionKind.TOP_LEVEL
            }
            !descriptor.name.isSpecial && !descriptor.name.asString().isEmpty() -> FunctionKind.MEMBER
            descriptor is AnonymousFunctionDescriptor -> FunctionKind.ANONYMOUS
            else -> throw IllegalStateException(
                "Unable to resolve FunctionKind for ${descriptor.fqNameSafe}, $ExceptionMessage"
            )
        }
    }

    override val isAbstract: Boolean by lazy {
        this.modifiers.contains(Modifier.ABSTRACT)
    }

    override val modifiers: Set<Modifier> by lazy {
        val modifiers = mutableSetOf<Modifier>()
        modifiers.addAll(descriptor.toKSModifiers())
        modifiers.addAll(descriptor.toFunctionKSModifiers())

        if (this.origin == Origin.JAVA_LIB) {
            if (this.jvmAccessFlag and Opcodes.ACC_STRICT != 0)
                modifiers.add(Modifier.JAVA_STRICT)
            if (this.jvmAccessFlag and Opcodes.ACC_SYNCHRONIZED != 0)
                modifiers.add(Modifier.JAVA_SYNCHRONIZED)
        }

        modifiers
    }

    override val parameters: List<KSValueParameter> by lazy {
        descriptor.valueParameters.map { KSValueParameterDescriptorImpl.getCached(it, this) }
    }

    override val returnType: KSTypeReference? by lazy {
        val returnType = descriptor.returnType
        if (returnType == null) {
            null
        } else {
            KSTypeReferenceDescriptorImpl.getCached(returnType, origin, this)
        }
    }

    override fun <D, R> accept(visitor: KSVisitor<D, R>, data: D): R {
        return visitor.visitFunctionDeclaration(this, data)
    }

    override fun asMemberOf(containing: KSType): KSFunction =
        ResolverImpl.instance!!.asMemberOf(this, containing)
}
