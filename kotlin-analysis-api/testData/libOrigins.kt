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

// TEST PROCESSOR: LibOriginsProcessor
// EXPECTED:
// Validating Anno1
// Validating Anno2
// Validating Anno3
// Validating Anno4
// Validating JavaLib
// Validating KotlinLibClass
// Exception: [KotlinLibClass, T1, Any?]: SYNTHETIC
// Exception: [KotlinLibClass, T1, Any?]: SYNTHETIC
// Validating kotlinLibFuntion
// Validating kotlinLibProperty
// Validating File: JavaSrc.java
// Exception: [File: JavaSrc.java, JavaSrc, synthetic constructor for JavaSrc, JavaSrc]: SYNTHETIC
// Exception: [File: JavaSrc.java, JavaSrc, synthetic constructor for JavaSrc]: SYNTHETIC
// Validating File: KotlinSrc.kt
// Exception: [File: KotlinSrc.kt, kotlinSrcProperty, kotlinSrcProperty.getter(), Short, Short]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, kotlinSrcProperty, kotlinSrcProperty.getter(), Short]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, kotlinSrcProperty, kotlinSrcProperty.getter()]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, Any]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, T3, Any?]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, q1, q1.getter(), Set<T3>]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, q1, q1.getter()]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, q2, q2.getter(), Short]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, q2, q2.getter()]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, q3, q3.getter(), Short, Short]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, q3, q3.getter(), Short]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, q3, q3.getter()]: SYNTHETIC
// Exception: [File: KotlinSrc.kt, KotlinSrcClass, T3, Any?]: SYNTHETIC
// END
// MODULE: module1
// FILE: KotlinLib.kt
package foo.bar

val kotlinLibProperty: Int = 0
fun kotlinLibFuntion(): Int = 0

annotation class Anno1
annotation class Anno2
annotation class Anno3
annotation class Anno4

@Anno1
class KotlinLibClass<T1>(val p1: List<T1>, val p2: Int)  {
    val p3: Int = 0
    fun f1(p4: T1): Int = 0
    fun f2(p5: List<T1>): Int = 0
    fun f3(p6: List<Int>): Int = 0
}

// FILE: JavaLib.java
package foo.bar;

import java.util.ArrayList;

@Anno2
class JavaLib<T2> {
    Byte javaLibField = 0;
    Byte javaLibFunction() {
        return 0;
    }
    Byte f1(T2 p0, ArrayList<T2> p1) {
        return 0;
    }
}

// MODULE: main(module1)
// FILE: KotlinSrc.kt
package foo.bar
val kotlinSrcProperty: Short = 0
fun kotlinSrcFuntion(): Short = 0

@Anno3
class KotlinSrcClass<T3>(val q1: Set<T3>, val q2: Short)  {
    val q3: Short = 0
    fun g1(q4: T3): Short = 0
    fun g2(q5: Set<T3>): Short = 0
    fun g3(q6: Set<Short>): Short = 0
}

// FILE: JavaSrc.java
package foo.bar;

import java.util.LinkedList;

@Anno4
class JavaSrc {
    Long javaSrcField = 0;
    Long javaSrcFunction() {
        return 0;
    }
    Long f2<T4>(T4 p0, LinkedList<T4> p1) {
        return 0;
    }
}

