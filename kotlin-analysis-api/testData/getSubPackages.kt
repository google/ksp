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

// TEST PROCESSOR: GetSubPackagesProcessor
// EXPECTED:
// subpackages of lib1
// subpackages of lib2
// subpackages of main
// main.test
// main.test.nested
// main.test.main
// main.test.main.test
// main.test.main.test.nested
// subpackages of non.exist
// END

// MODULE: lib1
// FILE: foo.kt
package lib1

class Foo

fun funcFoo(): Int {
    return 1
}

// FILE: Bar.java
package lib1;

class Bar {}

// MODULE: lib2
// FILE: foo.kt
package lib2

class Foo

typealias FooTypeAlias = Foo

val a = 0

// FILE: Bar.java

class Bar {}

// MODULE: main(lib1, lib2)
// FILE: a.kt
package lib1
class FooInSource

val propInSource = 1
// FILE: main.kt
package main.test

class KotlinMain

// FILE: main/test/C.java
package main.test;

public class C {

}

class D {

}

// FILE: wrongDir/K.java
package main;

public class K {

}

class KK {}


// FILE: main/test/main/test/L.java
package main.test;

public class L {

}


// FILE: main/test/main/test/nested/M.java
package main.test.nested;

public class M {

}
