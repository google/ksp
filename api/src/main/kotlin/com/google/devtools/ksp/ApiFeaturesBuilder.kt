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

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Returns a new immutable [ApiFeatures] by configuring [MutableApiFeatures] using the
 * given [builderAction].
 *
 * The [MutableApiFeatures] passed as a receiver to the [builderAction] is valid only inside that function and
 * must not be used outside it. In other words, avoid passing the `this` reference anywhere.
 */
@OptIn(ExperimentalContracts::class)
fun buildApiFeatures(builderAction: MutableApiFeatures.() -> Unit): ApiFeatures {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return MutableApiFeatures().let { builder ->
        builder.builderAction()
        KSApiFeatures(builder.enableBackingFields, builder.enableContextParameters)
    }
}

/**
 * A mutable copy of [ApiFeatures].
 * The default behavior is to disable all features to preserve backwards compatibility.
 * [MutableApiFeatures] does importantly not implement [ApiFeatures] to encourage
 * the use of [buildApiFeatures] and more generally to discourage the use of mutability for the [ApiFeatures]
 * type.
 */
class MutableApiFeatures(
    var enableBackingFields: Boolean = false,
    var enableContextParameters: Boolean = false,
)
