/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o.
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
// TEST PROCESSOR: HelloProcessor
// EXPECTED:
// 8
// test.Foo
// test.ITF
// Bar.list
// Bar.BBB
// AClass
// C
// C.f
// C.javaFun
// END
//FILE: a.kt
package test
annotation class Anno


@Anno
class Foo() {
    val k = "123"
    var a : String = "123"
    val aaa : (Int) -> Int = { a -> 1 }
    fun bar(): Int {
//        val aa = 1234
        return 3
    }
}

@Anno
interface ITF<T> {
    fun fooITF() = 1
}

//FILE: b.kt
import test.Anno
import test.ITF

class Bar<out S, out D>() : ITF<D> {
    @Anno
    val list : List<Int>? = null
    val funInObj = foo()
    open internal fun foo(c: C, dd: () -> D): Int {
        val a = 1
//        fun <TTT> foo(c: C, dd: () -> TTT): Int {
//            return 1
//        }
        return 1
    }
    @Anno
    class BBB {
        fun <TTA: String> fofo(c: C, dd: () -> TTA): Int {
            return 1
        }
        fun <TTA: Int> fofofo(c: C, dd: () -> TTA): Int {
            return 1
        }
    }
    val a = 1

    val kk
    get() = 1

    companion object {
        val s = 1
        fun foo() = 123
    }
}

//FILE: c.kt
import test.Anno

@Anno
class AClass(val a: Int, val b: String, c: Double) {
    fun foo() = a + b.length + c
}

fun <D> foo(c: C, dd: () -> D) = 1

class CC: C() {}

// FILE: C.java
import java.util.List;
import java.util.ArrayList;
import test.Foo;
import test.ITF;
import test.Anno;

@Anno
class C {
    @Anno
    public Foo f = new Foo();
    List<? extends ITF> list = new ArrayList<>();

    @Anno
    public String javaFun() {
        return f.k;
    }
}
