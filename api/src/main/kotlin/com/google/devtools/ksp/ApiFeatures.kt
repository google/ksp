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

/**
 * The [ApiFeatures] interface enumerates the different features a single processor may choose to enable
 * or disable. The [ApiFeatures] instance must be configured by the
 * [SymbolProcessorProvider][com.google.devtools.ksp.processing.SymbolProcessorProvider] upon creating the
 * [SymbolProcessor][com.google.devtools.ksp.processing.SymbolProcessor] (processor) and must be immutable.
 * If the [ApiFeatures] object is changed during processing, the changes are not guaranteed to propagate.
 * In other words, the [ApiFeatures] object must be created only once along with the creation of the processor,
 * and is a read-only data object that is passed around during processing.
 *
 * The [ApiFeatures] applies per processor, i.e., it only applies to the processor it is created alongside.
 * Thus, different processors may use different configurations without any risk of overriding each other's
 * configuration.
 *
 * The [ApiFeatures] type is an interface such that processor implementations may choose to freely implement the
 * type however they wish. However, KSP provides [buildApiFeatures] for convenience.
 *
 * There is an underlying assumption that processors will eventually support all features in this type, i.e.,
 * enabling all features, since at a future date KSP may decide to always enable them.
 * That is, when the feature is mature/stable then KSP may decide to ignore the feature toggle.
 */
interface ApiFeatures {
    val enableBackingFields: Boolean
        get() = false

    val enableContextParameters: Boolean
        get() = false
}
