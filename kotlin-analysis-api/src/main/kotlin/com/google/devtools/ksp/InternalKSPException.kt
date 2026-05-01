/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

package com.google.devtools.ksp

import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.Location
import com.google.devtools.ksp.symbol.NonExistLocation

/**
 * Internal exception that signals a bug in KSP.
 *
 * Copy of InternalKSPException in API package which is also marked
 * internal to avoid users depending on the type.
 * Importantly, this class is in a different package than in the API module
 * to avoid classpath collisions/shadowing.
 */
internal class InternalKSPException(
    message: String,
    val location: Location,
    val originatingClass: Class<*>
) : Exception(
    buildString {
        appendLine(">>> Internal KSP Error")
        appendLine("   | *** THIS IS A BUG IN KSP ***")
        appendLine("   |")
        message.lines().forEach { messageLine ->
            appendLine("   | $messageLine")
        }
        appendLine("   |")
        appendLine("   | Location           : ${location.render()}")
        appendLine("   | Class at occurrence: $originatingClass")
        appendLine("   |")
        appendLine("   | You can report it at https://github.com/google/ksp/issues/new")
        appendLine("   |")
    }
) {
    companion object {
        fun Location.render(): String {
            return when (this) {
                is FileLocation -> "${this.filePath}:${this.lineNumber}"
                is NonExistLocation -> "<unknown location>"
            }
        }
    }
}
