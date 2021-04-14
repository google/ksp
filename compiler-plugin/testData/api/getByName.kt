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

// TEST PROCESSOR: GetByNameProcessor
// EXPECTED:
// all success
// END
// MODULE: lib1
// FILE: foo.kt
package lib1

open class Foo {
    fun lib1MemberFun() = 1
    fun overload(a: Int) = "Overload"
    fun overload() = "Overload"
    val lib1MemberProp = 1.0
}

fun lib1TopFun(): Int {
    return 1
}

val lib1TopProp = "1"

// FILE: Bar.java
package lib1;

class Bar {
    public int lib1JavaMemberFun() {
        return 1;
    }
}

// MODULE: lib2
// FILE: foo.kt
package lib2

class Foo  {
    fun lib2MemberFun() = 1
    val lib2MemberProp = 1.0
}

// MODULE: main(lib1, lib2)
// FILE: a.kt
package source

class FooInSource {
    fun sourceMemberFun() = 1
    val sourceMemberProp = 1.0
}

val propInSource = 1
// FILE: main.kt
package main
import lib1.Foo

class KotlinMain : Foo {
    fun lib1MemberFun(a: Int) = 1
}

