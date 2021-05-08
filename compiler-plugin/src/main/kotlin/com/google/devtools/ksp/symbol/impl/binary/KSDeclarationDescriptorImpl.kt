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

import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.impl.kotlin.KSNameImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.parents

abstract class KSDeclarationDescriptorImpl(descriptor: DeclarationDescriptor) : KSDeclaration {

    override val origin = Origin.CLASS

    override val containingFile: KSFile? = null

    override val location: Location = NonExistLocation

    override val annotations: Sequence<KSAnnotation> by lazy {
        descriptor.annotations.asSequence().map { KSAnnotationDescriptorImpl.getCached(it) }
    }

    override val parentDeclaration: KSDeclaration? by lazy {
        val containingDescriptor = descriptor.parents.first()
        when (containingDescriptor) {
            is ClassDescriptor -> KSClassDeclarationDescriptorImpl.getCached(containingDescriptor)
            is FunctionDescriptor -> KSFunctionDeclarationDescriptorImpl.getCached(containingDescriptor)
            else -> null
        } as KSDeclaration?
    }

    override val packageName: KSName by lazy {
        KSNameImpl.getCached(descriptor.findPackage().fqName.asString())
    }

    override val qualifiedName: KSName by lazy {
        KSNameImpl.getCached(descriptor.fqNameSafe.asString())
    }

    override val simpleName: KSName by lazy {
        KSNameImpl.getCached(descriptor.name.asString())
    }

    override fun toString(): String {
        return this.simpleName.asString()
    }

}
