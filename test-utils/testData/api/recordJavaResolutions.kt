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
// TEST PROCESSOR: RecordJavaProcessor
// EXPECTED:
// p1.J1: main/p1/TestJ2J.java
// p1.J3: main/p1/TestJ2J.java
// p1.K1: main/p1/TestJ2K.java
// p1.K3: main/p1/TestJ2K.java
// p2.J2: main/p1/TestJ2J.java
// p2.K2: main/p1/TestJ2K.java
// p3.J3: main/p1/TestJ2J.java
// p3.K3: main/p1/TestJ2K.java
// END

// FILE: p1/TestJ2K.java
package p1;

import p2.K2;
import p3.*;

public class TestJ2K {
    K1 k1 = null;
    K2 k2 = null;
    K3 k3 = null;
}

// FILE: p1/TestJ2J.java
package p1;

import p2.J2;
import p3.*;

public class TestJ2J {
    J1 j1 = null;
    J2 j2 = null;
    J3 j3 = null;
}

// FILE: p1/K1.kt
package p1
class K1
// FILE: p1/K2.kt
package p1
class K2
// FILE: p2/K2.kt
package p2
class K2
// FILE: p3/K1.kt
package p3
class K1
// FILE: p3/K2.kt
package p3
class K2
// FILE: p3/K3.kt
package p3
class K3
// FILE: p1/J1.java
package p1;
public class J1 {}
// FILE: p1/J2.java
package p1;
public class J2 {}
// FILE: p2/J2.java
package p2;
public class J2 {}
// FILE: p3/J1.java
package p3;
public class J1 {}
// FILE: p3/J2.java
package p3;
public class J2 {}
// FILE: p3/J3.java
package p3;
public class J3 {}
