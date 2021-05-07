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
// annotation: Anno1: KOTLIN_LIB
// annotation: Anno2: JAVA_LIB
// annotation: Anno3: KOTLIN
// annotation: Anno4: JAVA
// classifier ref: Anno1: KOTLIN_LIB
// classifier ref: Anno1: KOTLIN_LIB
// classifier ref: Anno2: JAVA_LIB
// classifier ref: Anno2: KOTLIN_LIB
// classifier ref: Anno3: KOTLIN
// classifier ref: Anno3: KOTLIN_LIB
// classifier ref: Anno4: KOTLIN_LIB
// classifier ref: Annotation: KOTLIN_LIB
// classifier ref: Annotation: KOTLIN_LIB
// classifier ref: Annotation: KOTLIN_LIB
// classifier ref: Annotation: KOTLIN_LIB
// classifier ref: Any: JAVA_LIB
// classifier ref: Any: JAVA_LIB
// classifier ref: Any: JAVA_LIB
// classifier ref: Any: KOTLIN_LIB
// classifier ref: Any: KOTLIN_LIB
// classifier ref: Any: KOTLIN_LIB
// classifier ref: ArrayList<(T2..T2?)>: JAVA_LIB
// classifier ref: Byte: JAVA_LIB
// classifier ref: Byte: JAVA_LIB
// classifier ref: Byte: JAVA_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: Int: KOTLIN_LIB
// classifier ref: JavaLib: JAVA_LIB
// classifier ref: JavaLib: JAVA_LIB
// classifier ref: JavaLib: JAVA_LIB
// classifier ref: JavaLib<T2>: JAVA_LIB
// classifier ref: KotlinLibClass: KOTLIN_LIB
// classifier ref: KotlinLibClass: KOTLIN_LIB
// classifier ref: KotlinLibClass: KOTLIN_LIB
// classifier ref: KotlinLibClass: KOTLIN_LIB
// classifier ref: KotlinLibClass: KOTLIN_LIB
// classifier ref: KotlinLibClass: KOTLIN_LIB
// classifier ref: KotlinLibClass<T1>: KOTLIN_LIB
// classifier ref: KotlinSrcClass: SYNTHETIC
// classifier ref: List<Int>: KOTLIN_LIB
// classifier ref: List<T1>: KOTLIN_LIB
// classifier ref: List<T1>: KOTLIN_LIB
// classifier ref: List<T1>: KOTLIN_LIB
// classifier ref: List<T1>: KOTLIN_LIB
// classifier ref: Long: JAVA
// classifier ref: Long: JAVA
// classifier ref: Long: JAVA
// classifier ref: Object: JAVA
// classifier ref: Set: KOTLIN
// classifier ref: Set: KOTLIN
// classifier ref: Set: KOTLIN
// classifier ref: Set: KOTLIN
// classifier ref: Set<T3>: SYNTHETIC
// classifier ref: Short: KOTLIN
// classifier ref: Short: KOTLIN
// classifier ref: Short: KOTLIN
// classifier ref: Short: KOTLIN
// classifier ref: Short: KOTLIN
// classifier ref: Short: KOTLIN
// classifier ref: Short: KOTLIN
// classifier ref: Short: KOTLIN
// classifier ref: Short: KOTLIN
// classifier ref: Short: SYNTHETIC
// classifier ref: Short: SYNTHETIC
// classifier ref: Short: SYNTHETIC
// classifier ref: T1: KOTLIN_LIB
// classifier ref: T1: KOTLIN_LIB
// classifier ref: T1: KOTLIN_LIB
// classifier ref: T1: KOTLIN_LIB
// classifier ref: T1: KOTLIN_LIB
// classifier ref: T1: KOTLIN_LIB
// classifier ref: T2: JAVA_LIB
// classifier ref: T2: JAVA_LIB
// classifier ref: T2: JAVA_LIB
// classifier ref: T3: KOTLIN
// classifier ref: T3: KOTLIN
// classifier ref: T3: KOTLIN
// classifier ref: T3: KOTLIN
// classifier ref: T3: SYNTHETIC
// classifier ref: T4: JAVA
// classifier ref: T4: JAVA
// declaration: <init>: KOTLIN
// declaration: T3: KOTLIN
// declaration: foo.bar.Anno1.<init>: KOTLIN_LIB
// declaration: foo.bar.Anno1: KOTLIN_LIB
// declaration: foo.bar.Anno2.<init>: KOTLIN_LIB
// declaration: foo.bar.Anno2: KOTLIN_LIB
// declaration: foo.bar.Anno3.<init>: KOTLIN_LIB
// declaration: foo.bar.Anno3: KOTLIN_LIB
// declaration: foo.bar.Anno4.<init>: KOTLIN_LIB
// declaration: foo.bar.Anno4: KOTLIN_LIB
// declaration: foo.bar.JavaLib.<init>: JAVA_LIB
// declaration: foo.bar.JavaLib.T2: JAVA_LIB
// declaration: foo.bar.JavaLib.T2: JAVA_LIB
// declaration: foo.bar.JavaLib.f1: JAVA_LIB
// declaration: foo.bar.JavaLib.javaLibField: JAVA_LIB
// declaration: foo.bar.JavaLib.javaLibFunction: JAVA_LIB
// declaration: foo.bar.JavaLib: JAVA_LIB
// declaration: foo.bar.JavaSrc.<init>: SYNTHETIC
// declaration: foo.bar.JavaSrc.LinkedList: JAVA
// declaration: foo.bar.JavaSrc.f2: JAVA
// declaration: foo.bar.JavaSrc.javaSrcField: JAVA
// declaration: foo.bar.JavaSrc.javaSrcFunction: JAVA
// declaration: foo.bar.JavaSrc.p0: JAVA
// declaration: foo.bar.JavaSrc: JAVA
// declaration: foo.bar.KotlinLibClass.<init>: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass.T1: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass.T1: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass.f1: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass.f2: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass.f3: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass.p1: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass.p2: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass.p3: KOTLIN_LIB
// declaration: foo.bar.KotlinLibClass: KOTLIN_LIB
// declaration: foo.bar.KotlinSrcClass.g1: KOTLIN
// declaration: foo.bar.KotlinSrcClass.g2: KOTLIN
// declaration: foo.bar.KotlinSrcClass.g3: KOTLIN
// declaration: foo.bar.KotlinSrcClass.q1: KOTLIN
// declaration: foo.bar.KotlinSrcClass.q2: KOTLIN
// declaration: foo.bar.KotlinSrcClass.q3: KOTLIN
// declaration: foo.bar.KotlinSrcClass: KOTLIN
// declaration: foo.bar.kotlinLibFuntion: KOTLIN_LIB
// declaration: foo.bar.kotlinLibProperty: KOTLIN_LIB
// declaration: foo.bar.kotlinSrcFuntion: KOTLIN
// declaration: foo.bar.kotlinSrcProperty: KOTLIN
// property accessor: kotlinLibProperty.getter(): KOTLIN_LIB
// property accessor: kotlinSrcProperty.getter(): SYNTHETIC
// property accessor: p1.getter(): KOTLIN_LIB
// property accessor: p2.getter(): KOTLIN_LIB
// property accessor: p3.getter(): KOTLIN_LIB
// property accessor: q1.getter(): SYNTHETIC
// property accessor: q2.getter(): SYNTHETIC
// property accessor: q3.getter(): SYNTHETIC
// reference: Anno1: KOTLIN_LIB
// reference: Anno1: KOTLIN_LIB
// reference: Anno2: JAVA_LIB
// reference: Anno2: KOTLIN_LIB
// reference: Anno3: KOTLIN
// reference: Anno3: KOTLIN_LIB
// reference: Anno4: JAVA
// reference: Anno4: KOTLIN_LIB
// reference: Annotation: KOTLIN_LIB
// reference: Annotation: KOTLIN_LIB
// reference: Annotation: KOTLIN_LIB
// reference: Annotation: KOTLIN_LIB
// reference: Any: JAVA_LIB
// reference: Any: JAVA_LIB
// reference: Any: JAVA_LIB
// reference: Any: KOTLIN_LIB
// reference: Any: KOTLIN_LIB
// reference: Any: KOTLIN_LIB
// reference: ArrayList<(T2..T2?)>: JAVA_LIB
// reference: Byte: JAVA_LIB
// reference: Byte: JAVA_LIB
// reference: Byte: JAVA_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: Int: KOTLIN_LIB
// reference: JavaLib<T2>: JAVA_LIB
// reference: JavaSrc: SYNTHETIC
// reference: KotlinLibClass<T1>: KOTLIN_LIB
// reference: KotlinSrcClass<T3>: KOTLIN
// reference: List<Int>: KOTLIN_LIB
// reference: List<T1>: KOTLIN_LIB
// reference: List<T1>: KOTLIN_LIB
// reference: List<T1>: KOTLIN_LIB
// reference: List<T1>: KOTLIN_LIB
// reference: Long: JAVA
// reference: Long: JAVA
// reference: Long: JAVA
// reference: Object: JAVA
// reference: Set: KOTLIN
// reference: Set: KOTLIN
// reference: Set: KOTLIN
// reference: Set: KOTLIN
// reference: Set<T3>: SYNTHETIC
// reference: Short: KOTLIN
// reference: Short: KOTLIN
// reference: Short: KOTLIN
// reference: Short: KOTLIN
// reference: Short: KOTLIN
// reference: Short: KOTLIN
// reference: Short: KOTLIN
// reference: Short: KOTLIN
// reference: Short: KOTLIN
// reference: Short: SYNTHETIC
// reference: Short: SYNTHETIC
// reference: Short: SYNTHETIC
// reference: T1: KOTLIN_LIB
// reference: T1: KOTLIN_LIB
// reference: T1: KOTLIN_LIB
// reference: T1: KOTLIN_LIB
// reference: T1: KOTLIN_LIB
// reference: T1: KOTLIN_LIB
// reference: T2: JAVA_LIB
// reference: T2: JAVA_LIB
// reference: T2: JAVA_LIB
// reference: T3: KOTLIN
// reference: T3: KOTLIN
// reference: T3: KOTLIN
// reference: T3: KOTLIN
// reference: T3: SYNTHETIC
// reference: T4: JAVA
// reference: T4: JAVA
// type arg: INVARIANT Int: KOTLIN_LIB
// type arg: INVARIANT Short: KOTLIN
// type arg: INVARIANT T1: KOTLIN_LIB
// type arg: INVARIANT T1: KOTLIN_LIB
// type arg: INVARIANT T1: KOTLIN_LIB
// type arg: INVARIANT T1: KOTLIN_LIB
// type arg: INVARIANT T1: KOTLIN_LIB
// type arg: INVARIANT T2: JAVA_LIB
// type arg: INVARIANT T2: JAVA_LIB
// type arg: INVARIANT T3: KOTLIN
// type arg: INVARIANT T3: KOTLIN
// type arg: INVARIANT T3: KOTLIN
// type arg: INVARIANT T3: SYNTHETIC
// value param: p0: JAVA_LIB
// value param: p1: JAVA_LIB
// value param: p1: KOTLIN_LIB
// value param: p2: KOTLIN_LIB
// value param: p4: KOTLIN_LIB
// value param: p5: KOTLIN_LIB
// value param: p6: KOTLIN_LIB
// value param: q1: KOTLIN
// value param: q2: KOTLIN
// value param: q4: KOTLIN
// value param: q5: KOTLIN
// value param: q6: KOTLIN
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

