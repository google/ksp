/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.binary

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.psi.KtClassOrObject

class KSEnumEntryDeclarationDescriptorImpl(val descriptor: ClassDescriptor) : KSEnumEntryDeclaration,
    KSClassDeclaration by KSClassDeclarationDescriptorImpl.getCached(descriptor) {
    companion object {
        val cache = mutableMapOf<ClassDescriptor, KSEnumEntryDeclarationDescriptorImpl>()
        fun getCached(descriptor: ClassDescriptor) = cache.getOrPut(descriptor) { KSEnumEntryDeclarationDescriptorImpl(descriptor) }
    }
}
