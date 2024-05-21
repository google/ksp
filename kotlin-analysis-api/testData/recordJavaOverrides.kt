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
// TEST PROCESSOR: RecordJavaOverridesProcessor
// EXPECTED:
// p1.B: main/p1/A.java
// p1.C: main/p1/B.java
// p1.R1: main/p1/A.java
// p1.R1: main/p1/C.java
// p1.R2: main/p1/A.java
// p1.R2: main/p1/C.java
// p1.V1: main/p1/A.java
// p1.V1: main/p1/C.java
// p1.V2: main/p1/A.java
// p1.V2: main/p1/C.java
// END

// FILE: p1/A.java
package p1;
public class A extends B {
    R1 f1(V1 v) {
        return null
    }

    R2 f2(V2 v) {
        return null
    }
}

// FILE: p1/B.java
package p1;
public class B extends C {
    R1 f1(V1 v) {
        return null
    }
}

// FILE: p1/C.java
package p1;
public class C extends D {
    R1 f1(V1 v) {
        return null
    }

    R2 f2(V2 v) {
        return null
    }
}

// FILE: p1/D.kt
package p1;

class V1
class V2
class R1
class R2
