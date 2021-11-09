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
// TEST PROCESSOR: EquivalentJavaWildcardProcessor
// EXPECTED:
// <init> : A1 -> A1
// <init> : A<T1, T2> -> A<T1, T2>
// <init> : C1 -> C1
// <init> : C2 -> C2
// <init> : C3 -> C3
// <init> : C4 -> C4
// <init> : X -> X
// <init> : Y -> Y
// @JvmSuppressWildcards : JvmSuppressWildcards -> JvmSuppressWildcards
// @JvmSuppressWildcards : JvmSuppressWildcards -> JvmSuppressWildcards
// @JvmWildcard : JvmWildcard -> JvmWildcard
// C1 : A<X, X> -> A<X, X>
// C2 : A<Any, Y> -> A<Any, Y>
// C3 : B<X> -> B<X>
// C4 : B<A<X, out Y>> -> B<A<in X, out Y>>
// Y : X -> X
// a1 : A<in X, out X> -> A<in X, out X>
// a1 : A<in X, out X> -> A<in X, out X>
// a1.getter() : A<in X, out X> -> A<in X, out X>
// a2 : A<Any, Y> -> A<Any, Y>
// a2 : A<Any, Y> -> A<Any, Y>
// a2.getter() : A<Any, Y> -> A<in Any, out Y>
// a3 : A<*, *> -> A<out Any?, out Any?>
// a3 : A<*, *> -> A<out Any?, out Any?>
// a3.getter() : A<*, *> -> A<out Any?, out Any?>
// a4 : B<X> -> B<X>
// a4 : B<X> -> B<X>
// a4.getter() : B<X> -> B<X>
// a5 : B<in X> -> B<in X>
// a5 : B<in X> -> B<in X>
// a5.getter() : B<in X> -> B<in X>
// a6 : B<out X> -> B<out X>
// a6 : B<out X> -> B<out X>
// a6.getter() : B<out X> -> B<out X>
// a7 : B<*> -> B<out Any?>
// a7 : B<*> -> B<out Any?>
// a7.getter() : B<*> -> B<out Any?>
// bar1 : [@kotlin.jvm.JvmSuppressWildcards] A<X, X> -> [@kotlin.jvm.JvmSuppressWildcards] A<X, X>
// bar2 : [@kotlin.jvm.JvmSuppressWildcards] A<X, X> -> [@kotlin.jvm.JvmSuppressWildcards] A<in X, out X>
// bar3 : [@kotlin.jvm.JvmWildcard] A<X, X> -> [@kotlin.jvm.JvmWildcard] A<in X, out X>
// foo : Unit -> Unit
// p1 : A<in X, out X> -> A<X, X>
// p1.getter() : A<in X, out X> -> A<X, X>
// p2 : A<Any, Y> -> A<Any, Y>
// p2.getter() : A<Any, Y> -> A<Any, Y>
// p3 : A<*, *> -> A<Any?, Any?>
// p3.getter() : A<*, *> -> A<Any?, Any?>
// p4 : B<X> -> B<X>
// p4.getter() : B<X> -> B<X>
// p5 : B<in X> -> B<in X>
// p5.getter() : B<in X> -> B<in X>
// p6 : B<out X> -> B<out X>
// p6.getter() : B<out X> -> B<out X>
// p7 : B<*> -> B<out Any?>
// p7.getter() : B<*> -> B<out Any?>
// r1 : A<X, X> -> A<X, X>
// r2 : A<Any, Y> -> A<Any, Y>
// r3 : A<*, *> -> A<Any?, Any?>
// r4 : B<X> -> B<X>
// r5 : B<in X> -> B<in X>
// r6 : B<out X> -> B<out X>
// r7 : B<*> -> B<out Any?>
// synthetic constructor for B : B<*> -> B<out Any?>
// v1 : A<X, X> -> A<in X, out X>
// v2 : A<Any, Y> -> A<Any, Y>
// v3 : A<*, *> -> A<out Any?, out Any?>
// v4 : B<X> -> B<X>
// v5 : B<in X> -> B<in X>
// v6 : B<out X> -> B<out X>
// v7 : B<*> -> B<out Any?>
// END

open class X()
final class Y() : X()

open class A<in T1, out T2>()
open class B<T>

// FIXME: should this annotation propagate to the return type?
// @JvmSuppressWildcards(false)
// fun bar(): A<X, X> = TODO()

fun bar1(): @JvmSuppressWildcards(true) A<X, X> = TODO()
fun bar2(): @JvmSuppressWildcards(false) A<X, X> = TODO()
fun bar3(): @JvmWildcard A<X, X> = TODO()

val p1: A<in X, out X> = TODO()
val p2: A<Any, Y> = TODO()
val p3: A<*, *> = TODO()
val p4: B<X> = TODO()
val p5: B<in X> = TODO()
val p6: B<out X> = TODO()
val p7: B<*> = TODO()

fun foo(
    v1: A<X, X>,
    v2: A<Any, Y>,
    v3: A<*, *>,
    v4: B<X>,
    v5: B<in X>,
    v6: B<out X>,
    v7: B<*>,
): Unit = Unit

fun r1(): A<X, X> = TODO()
fun r2(): A<Any, Y> = TODO()
fun r3(): A<*, *> = TODO()
fun r4(): B<X> = TODO()
fun r5(): B<in X> = TODO()
fun r6(): B<out X> = TODO()
fun r7(): B<*> = TODO()

class C1() : A<X, X>()
class C2() : A<Any, Y>()
class C3() : B<X>()
class C4() : B<A<X, out Y>>()

annotation class A1(
    val a1: A<in X, out X>,
    val a2: A<Any, Y>,
    val a3: A<*, *>,
    val a4: B<X>,
    val a5: B<in X>,
    val a6: B<out X>,
    val a7: B<*>
)
