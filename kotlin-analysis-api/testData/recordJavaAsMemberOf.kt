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
// TEST PROCESSOR: RecordJavaAsMemberOfProcessor
// EXPECTED:
// kotlin.Any: javaSrc/p1/B.java
// p1.A: javaSrc/p1/B.java
// p1.B: javaSrc/p1/A.java
// p1.C: javaSrc/p1/A.java
// p1.C: javaSrc/p1/B.java
// p1.D: javaSrc/p1/A.java
// p1.D: javaSrc/p1/B.java
// p1.E: javaSrc/p1/B.java
// END

// FILE: p1/A.java
package p1;
public class A<T> extends B<T, D> {
}

// FILE: p1/B.java
package p1;
public class B<T, R> {
    public <T extends D> R f(A<? super C> p, E p2) {
        return null;
    }
}

// FILE: p1/C.kt
package p1;
class C
class D
class E
val a = A<C>()
