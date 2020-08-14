/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ksp.gradle

import org.gradle.api.GradleException

open class KspExtension {
    internal val apOptions = mutableMapOf<String, String>()

    open fun arg(k: String, v: String) {
        if ('=' in k) {
            throw GradleException("'=' is not allowed in custom option's name.")
        }
        apOptions.put(k, v)
    }
}
