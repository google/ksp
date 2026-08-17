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
// TEST PROCESSOR: RecordJavaSupertypesProcessor
// EXPECTED:
// <anonymous>.A: main/A.java
// <anonymous>.B: main/A.java
// <anonymous>.C: main/A.java
// <anonymous>.C: main/C.java
// <anonymous>.D: main/C.java
// <anonymous>.D: main/D.java
// kotlin.Any: main/C.java
// kotlin.Any: main/D.java
// END

// FILE: A.java
public class A extends B<C<A>> {
}

// FILE: B.kt
open class B<T>() : C<T>()

// FILE: C.java
public class C<T> extends D {

}

// FILE: D.java
public class D {

}
