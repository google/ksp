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

// WITH_RUNTIME
// TEST PROCESSOR: InnerTypeProcessor
// EXPECTED:
// C1<*>: [C1<STAR Any>]
// C1<Int>: [C1<INVARIANT Int>]
// C2<*, *>: [C1.C2<STAR Any>, C1<STAR Any>]
// C2<Short, Int>: [C1.C2<INVARIANT Short>, C1<INVARIANT Int>]
// C3<*, *, *>: [C1.C2.C3<STAR Any>, C1.C2<STAR Any>, C1<STAR Any>]
// C3<Byte, Short, Int>: [C1.C2.C3<INVARIANT Byte>, C1.C2<INVARIANT Short>, C1<INVARIANT Int>]
// C4<*>: [C1.C4<STAR Any>]
// C4<Double>: [C1.C4<INVARIANT Double>]
// C5<*, *>: [C1.C4.C5<STAR Any>, C1.C4<STAR Any>]
// C5<Float, Double>: [C1.C4.C5<INVARIANT Float>, C1.C4<INVARIANT Double>]
// END

@file:Suppress("Byte", "Int", "Short", "Double", "Float", "Suppress", "Any")

class C1<T1> {
    inner class C2<T2> {
        inner class C3<T3> {

        }
    }

    class C4<T4> {
        inner class C5<T5>
    }
}

val c1 = C1<Int>()
val c2 = c1.C2<Short>()
val c3 = c2.C3<Byte>()
val c4 = C1.C4<Double>()
val c5 = c4.C5<Float>()
