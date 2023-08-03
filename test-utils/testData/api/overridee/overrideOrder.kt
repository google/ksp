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

// WITH_RUNTIME
// TEST PROCESSOR: OverrideeProcessor
// EXPECTED:
// OverrideOrder1:
// OverrideOrder1.foo() -> GrandBaseInterface2.foo()
// OverrideOrder2:
// OverrideOrder2.foo() -> GrandBaseInterface1.foo()
// END

// FILE: overrideOrder.kt
interface GrandBaseInterface1 {
    fun foo(): Unit
}

interface GrandBaseInterface2 {
    fun foo(): Unit
}

interface BaseInterface1 : GrandBaseInterface1 {
}

interface BaseInterface2 : GrandBaseInterface2 {
}

class OverrideOrder1 : BaseInterface1, GrandBaseInterface2 {
    override fun foo() = TODO()
}
class OverrideOrder2 : BaseInterface2, GrandBaseInterface1 {
    override fun foo() = TODO()
}
