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

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

class KspGradleLogger(val loglevel: Int) : KSPLogger {
    private val messager = System.out

    override fun logging(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_LOGGING)
            messager.println("v: [ksp] $message")
    }

    override fun info(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_INFO)
            messager.println("i: [ksp] $message")
    }

    override fun warn(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_WARN)
            messager.println("w: [ksp] $message")
    }

    override fun error(message: String, symbol: KSNode?) {
        if (loglevel <= LOGGING_LEVEL_ERROR)
            messager.println("e: [ksp] $message")
    }

    override fun exception(e: Throwable) {
        if (loglevel <= LOGGING_LEVEL_ERROR)
            messager.println("e: [ksp] $e")
    }

    companion object {
        const val LOGGING_LEVEL_LOGGING = 0
        const val LOGGING_LEVEL_INFO = 1
        const val LOGGING_LEVEL_WARN = 3
        const val LOGGING_LEVEL_ERROR = 5
    }
}
