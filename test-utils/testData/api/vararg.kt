/*
 * Copyright 2025 Google LLC
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: VarargProcessor
// EXPECTED:
// s.K.foo(vararg strings: String)
// s.J.foo(vararg strings: (String..String?))
// l.K.foo(vararg strings: String)
// l.J.foo(vararg p0: (String..String?))
// END

// MODULE: lib
// FILE: l/K.kt
package l
class K {
    fun foo(vararg strings: String) = 0
}

// FILE: l/J.java
package l;
public class J {
    int foo(String... strings) {
        return 0;
    }
}

// MODULE: main(lib)
// FILE: s/K.kt
package s
class K {
    fun foo(vararg strings: String) = 0
}

// FILE: s/J.java
package s;
public class J {
    int foo(String... strings) {
        return 0;
    }
}

