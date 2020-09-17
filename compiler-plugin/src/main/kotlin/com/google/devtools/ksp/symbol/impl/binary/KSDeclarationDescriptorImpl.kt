/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    override val annotations: List<KSAnnotation> by lazy {
        descriptor.annotations.map { KSAnnotationDescriptorImpl.getCached(it) }
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