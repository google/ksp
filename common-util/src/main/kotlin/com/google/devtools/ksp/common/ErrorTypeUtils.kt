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

package com.google.devtools.ksp.common

import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument

// This does not work when the type is already error AND args are either empty or matches actual size.
inline fun <E> errorTypeOnInconsistentArguments(
    arguments: List<KSTypeArgument>,
    placeholdersProvider: () -> List<KSTypeArgument>,
    withCorrectedArguments: (corrected: List<KSTypeArgument>) -> KSType,
    errorType: (name: String, message: String) -> E,
): E? {
    if (arguments.isNotEmpty()) {
        val placeholders = placeholdersProvider()
        val diff = arguments.size - placeholders.size
        if (diff > 0) {
            val wouldBeType = withCorrectedArguments(arguments.dropLast(diff))
            return errorType(wouldBeType.toString(), "Unexpected extra $diff type argument(s)")
        } else if (diff < 0) {
            val wouldBeType = withCorrectedArguments(arguments + placeholders.drop(arguments.size))
            return errorType(wouldBeType.toString(), "Missing ${-diff} type argument(s)")
        }
    }
    return null
}
