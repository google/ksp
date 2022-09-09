/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

class CommandLineKSPLogger : KSPLogger {
    // TODO: support logging level.
    private val messager = System.err
    override fun logging(message: String, symbol: KSNode?) {
        messager.println(message)
    }

    override fun info(message: String, symbol: KSNode?) {
        messager.println(message)
    }

    override fun warn(message: String, symbol: KSNode?) {
        messager.println(message)
    }

    override fun error(message: String, symbol: KSNode?) {
        messager.println(message)
    }

    override fun exception(e: Throwable) {
        messager.println(e.message)
    }
}
