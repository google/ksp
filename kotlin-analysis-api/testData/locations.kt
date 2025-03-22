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
// A:K.kt:51
// File: K.kt:K.kt:1
// K:J.java:73
// K:K.kt:54
// T:J.java:73
// T:J.java:73
// T:K.kt:54
// f1:J.java:77
// f1:K.kt:61
// p1:K.kt:56
// p2:J.java:77
// p2:K.kt:61
// v1:K.kt:55
// v1:K.kt:55
// v2:J.java:74
// v2:K.kt:58
// v3.getter():K.kt:66
// v3.setter():K.kt:67
// v3:J.java:82
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

    @Location
    List<Double> v3 = List<double>()
}
