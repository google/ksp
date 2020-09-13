/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.google.devtools.kotlin.symbol.processing.symbol.impl

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import com.google.devtools.kotlin.symbol.processing.symbol.impl.binary.KSAnnotationDescriptorImpl

class KSObjectCacheManager {
    companion object {
        val caches = arrayListOf<KSObjectCache<*, *>>()

        fun register(cache: KSObjectCache<*, *>) = caches.add(cache)
        fun clear() = caches.forEach { it.clear() }
    }
}

abstract class KSObjectCache<K, V> {
    val cache = mutableMapOf<K, V>()

    init {
        KSObjectCacheManager.register(this)
    }

    open fun clear() = cache.clear()
}
