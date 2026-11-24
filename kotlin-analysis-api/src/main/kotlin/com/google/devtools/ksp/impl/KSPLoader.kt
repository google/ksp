/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
package com.google.devtools.ksp.impl

import com.google.devtools.ksp.processing.KSPConfig
import com.google.devtools.ksp.processing.KspGradleLogger
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class KSPLoader {
    companion object {
        @JvmStatic
        fun loadAndRunKSP(
            kspConfigStream: ByteArray,
            processorProvider: List<SymbolProcessorProvider>,
            logLevel: Int
        ): Int = loadAndRunKSP(kspConfigStream, Supplier { processorProvider }, logLevel)

        /**
         * Like the overload above, but [processorProviders] is only called if there is something to process (see
         * [KotlinSymbolProcessing]).
         */
        @JvmStatic
        fun loadAndRunKSP(
            kspConfigStream: ByteArray,
            processorProviders: Supplier<List<SymbolProcessorProvider>>,
            logLevel: Int
        ): Int {
            val kspConfig = ObjectInputStream(ByteArrayInputStream(kspConfigStream)).use { objectInputStream ->
                objectInputStream.readObject() as KSPConfig
            }
            val ksp = KotlinSymbolProcessing(kspConfig, processorProviders, KspGradleLogger(logLevel))
            return ksp.execute().ordinal
        }

        /**
         * Shuts down background threads started inside the KSP2 engine classloader so that the
         * classloader (and any plugin classloader captured via thread contextClassLoader) can be
         * garbage collected after the build finishes.
         */
        @OptIn(DelicateCoroutinesApi::class)
        @JvmStatic
        fun shutdown() {
            var firstError: Throwable? = null
            try {
                AppExecutorUtil.shutdownApplicationScheduledExecutorService()
                AppExecutorUtil.getAppScheduledExecutorService().awaitTermination(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                firstError = e
            } catch (e: Throwable) {
                firstError = e
            }
            try {
                Dispatchers.shutdown()
            } catch (e: Throwable) {
                if (firstError == null) {
                    firstError = e
                } else {
                    firstError.addSuppressed(e)
                }
            }
            if (firstError != null) {
                throw firstError
            }
        }
    }
}
