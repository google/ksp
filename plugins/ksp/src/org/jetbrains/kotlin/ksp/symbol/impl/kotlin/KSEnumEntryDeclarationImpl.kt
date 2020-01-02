/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.symbol.impl.kotlin

import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.psi.KtClassOrObject

class KSEnumEntryDeclarationImpl(ktClassOrObject: KtClassOrObject) : KSEnumEntryDeclaration,
    KSClassDeclaration by KSClassDeclarationImpl.getCached(ktClassOrObject) {
    companion object {
        val cache = mutableMapOf<KtClassOrObject, KSEnumEntryDeclarationImpl>()
        fun getCached(ktClassOrObject: KtClassOrObject) = cache.getOrPut(ktClassOrObject) { KSEnumEntryDeclarationImpl(ktClassOrObject) }
    }
}
