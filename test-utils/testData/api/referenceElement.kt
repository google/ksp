/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// TEST PROCESSOR: ReferenceElementProcessor
// EXPECTED:
// KSClassifierReferenceImpl: Qualifier of B is A
// KSClassifierReferenceImpl: Qualifier of C is A
// KSClassifierReferenceImpl: Qualifier of Int is null
// KSClassifierReferenceImpl: Qualifier of String is null
// KSClassifierReferenceDescriptorImpl: Qualifier of Int is null
// KSClassifierReferenceDescriptorImpl: Qualifier of String is null
// KSClassifierReferenceDescriptorImpl: Qualifier of Y is X
// KSClassifierReferenceDescriptorImpl: Qualifier of Z<Int> is X<String>
// KSDefNonNullReferenceImpl: Enclosed type of T
// KSClassifierReferenceJavaImpl: Qualifier of H is J<String>
// KSClassifierReferenceJavaImpl: Qualifier of I is J
// KSClassifierReferenceJavaImpl: Qualifier of Object is null
// KSClassifierReferenceJavaImpl: Qualifier of Object is null
// KSClassifierReferenceJavaImpl: Qualifier of Object is null
// KSClassifierReferenceJavaImpl: Qualifier of Object is null
// KSClassifierReferenceJavaImpl: Qualifier of String is null
// END

// MODULE: lib
// FILE: lib.kt
class X<T1> {
    class Y
    inner class Z<T2>
}

val z: X.Y = X.Y()
val w: X<String>.Z<Int> = X<String>().Z<Int>()

// MODULE: main(lib)
// FILE: reference.kt
class A<T1> {
    class B
    inner class C<T2>
}

class DefNonNull<T> {
    val u: T & Any
}

val x: A.B = A.B()
val y: A<String>.C<Int> = A<String>().C<Int>()

// FILE: J.java
class J<T> {
    class H {
    }

    static class I {
    }
}

class K {
    J<String>.H x = null;
    J.I z = null;
}
