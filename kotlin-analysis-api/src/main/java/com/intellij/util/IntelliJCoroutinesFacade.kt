// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.ApiStatus
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

@ApiStatus.Internal
object IntelliJCoroutinesFacade {

    // Backport from intellij-community to resolve issue with intellij-fork of kotlinx coroutines.
    // The interface must be exactly the same as the upstream file.

    val canUseIntelliJCoroutines: Boolean = false

    fun currentThreadCoroutineContext(): CoroutineContext? {
        return null
    }

    @Throws(InterruptedException::class)
    fun <T> runBlockingWithParallelismCompensation(
        context: CoroutineContext,
        block: suspend CoroutineScope.() -> T
    ): T {
        @Suppress("RAW_RUN_BLOCKING")
        return runBlocking(context, block)
    }

    fun <T> runAndCompensateParallelism(timeout: Duration, action: () -> T): T {
        return action()
    }
}
