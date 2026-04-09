/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: LocationsProcessor
// EXPECTED:
// A:K.kt:57
// File: K.kt:K.kt:1
// K:J.java:81
// K:K.kt:60
// Q:J.java:89
// Q:K.kt:69
// T:J.java:81
// T:J.java:81
// T:K.kt:60
// delegateProp:K2.kt:110
// f1:J.java:85
// f1:K.kt:67
// fieldProp:K2.kt:105
// p1:K.kt:62
// p2:J.java:85
// p2:K.kt:67
// v1:K.kt:61
// v1:K.kt:61
// v2:J.java:82
// v2:K.kt:64
// v3.getter():K.kt:74
// v3.setter():K.kt:75
// v3:J.java:94
// value:K2.kt:113
// x1.getter():NonExistLocation
// END


// FILE: Location.kt

annotation class Location

// FILE: K.kt

@file:Location

@Location
typealias A = K

@Location
class K<@Location T>(
    @Location val v1: Int,
    @Location p1: String,
) {
    @Location val v2: Int

    @Location
    fun f1(@Location p2: Int) = Unit

    fun <@Location Q> f2() = Unit

    @get:Location
    @set:Location
    var v3: List<Double>
        get() = TODO()
        set(v: List<Double>) = Unit
}

// FILE: J.java

@Location
class K<@Location T> {
    @Location int v2 = 0;

    @Location
    void f1(@Location int p2) {

    }

    <@Location Q> void f2() {

    }

    @Location
    List<Double> v3 = List<double>()
}

// FILE: K1.kt

class A(@get:Location val x1: Int)

// FILE: K2.kt

class L {
    @field:Location
    val fieldProp: Int = 0

    private val backing = 42

    @delegate:Location
    val delegateProp: Int by backing

    var setParamProp: Int = 0
        set(@setparam:Location value) { field = value }
}

fun @receiver:Location String.extFun() = Unit

interface <A> Foo<A>

abstract class Abs {
    fun <A> bar1(baz: Foo<@Location A>): Foo<A>
    fun <A> bar2(baz: Foo<A>): Foo<@Location A>
}

// FILE: J2.java

interface <A> Foo2<A> {}

abstract class Abs2 {
    Foo2<String> bar1(Foo2<@Location Boolean> baz)
    Foo2<@Location A> bar2(Foo2<A> baz)
}

// FILE: ExpressionAnnotation.kt

val x = @Location listOf(1, 2, 3)
