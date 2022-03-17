/*
 * Copyright 2021 Google LLC
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

import org.jetbrains.kotlin.incremental.LookupTrackerImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.Position
import org.jetbrains.kotlin.incremental.components.ScopeKind

class DualLookupTracker : LookupTracker {
    val symbolTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)
    val classTracker = LookupTrackerImpl(LookupTracker.DO_NOTHING)

    override val requiresPosition: Boolean
        get() = symbolTracker.requiresPosition || classTracker.requiresPosition

    override fun record(filePath: String, position: Position, scopeFqName: String, scopeKind: ScopeKind, name: String) {
        symbolTracker.record(filePath, position, scopeFqName, scopeKind, name)
        if (scopeKind == ScopeKind.CLASSIFIER) {
            val className = scopeFqName.substringAfterLast('.')
            val outerScope = scopeFqName.substringBeforeLast('.', "<anonymous>")
            // DO NOT USE: ScopeKind is meaningless
            classTracker.record(filePath, position, outerScope, scopeKind, className)
        }
    }
}
