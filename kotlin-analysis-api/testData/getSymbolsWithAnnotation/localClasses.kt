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

// TEST PROCESSOR: GetSymbolsWithAnnotationProcessor
// EXPECTED:
// Outer
// Outer.<no name provided>
// Outer.Companion
// Outer.Inner
// Outer.Local
// Outer.PrivateInner
// Outer.PrivateLocal
// END

// FILE: Anno.kt
annotation class Anno

// FILE: LocalClasses.kt

@Anno
class Outer {
    @Anno
    class Local

    @Anno
    inner class Inner

    @Anno
    private class PrivateLocal

    @Anno
    private inner class PrivateInner

    @Anno
    object : Any {

    }

    @Anno
    companion object {

    }
}
