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
// A:K.kt:51,10-51,15
// File: K.kt:K.kt:1,0-70,0
// K:J.java:73,6-83,1
// K:K.kt:54,6-68,1
// T:J.java:73,8-73,22
// T:J.java:73,8-73,22
// T:K.kt:54,18-54,21
// f1:J.java:77,9-79,5
// f1:K.kt:61,8-61,36
// p1:K.kt:56,14-56,25
// p2:J.java:77,26-77,31
// p2:K.kt:61,21-61,36
// v1:K.kt:55,18-55,26
// v1:K.kt:55,18-55,26
// v2:J.java:74,18-74,25
// v2:K.kt:58,18-58,25
// v3.getter():K.kt:66,8-66,22
// v3.setter():K.kt:67,8-67,35
// v3:J.java:82,17-82,36
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
