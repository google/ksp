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
// TEST PROCESSOR: AnnotationsRepeatableProcessor
// EXPECTED:
// KTest, A, [value:1]}
// KTest, A, [value:2]}
// KTest, KA, [value:1]}
// KTest, KA, [value:2]}
// lib.KTest, C, [value:[@A, @A]]}
// lib.KTest, KA, [value:1]}
// lib.KTest, KA, [value:2]}
// Test, A, [value:1]}
// Test, A, [value:2]}
// Test, KA, [value:1]}
// Test, KA, [value:2]}
// lib.Test, Container, [value:[@KA, @KA]]}
// lib.Test, C, [value:[@A, @A]]}
// END

// MODULE: lib
// FILE: placeholder.kt
// FILE: lib/A.java
package lib;
import java.lang.annotation.Repeatable;
@Repeatable(A.C.class)
@interface A {
    int value();
    @interface C {
        A[] value();
    }
}
// FILE: lib/KA.kt
package lib
@Repeatable
annotation class KA(val value: Int)

// FILE: lib/KTest.kt
package lib
@A(1)
@A(2)
@KA(1)
@KA(2)
class KTest
// FILE: lib/Test.java
package lib;
@A(1)
@A(2)
@KA(1)
@KA(2)
class Test {}
// MODULE: main(lib)
// FILE: KTest.kt
import lib.*
@A(1)
@A(2)
@KA(1)
@KA(2)
class KTest
// FILE: Test.java
import lib.*;
@A(1)
@A(2)
@KA(1)
@KA(2)
class Test {}
