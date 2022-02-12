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
// <init> : X -> X
// Y : X -> X
// <init> : Y -> Y
// <init> : A<T1, T2> -> A<T1, T2>
// synthetic constructor for B : B<*> -> B<out Any?>
// bar1 : [@kotlin.jvm.JvmSuppressWildcards] A<X, X> -> [@kotlin.jvm.JvmSuppressWildcards] A<X, X>
// - INVARIANT X : X -> X
// - INVARIANT X : X -> X
// - @JvmSuppressWildcards : JvmSuppressWildcards -> JvmSuppressWildcards
// bar2 : [@kotlin.jvm.JvmSuppressWildcards] A<X, X> -> [@kotlin.jvm.JvmSuppressWildcards] A<in X, out X>
// - INVARIANT X : X -> X
// - INVARIANT X : X -> X
// - @JvmSuppressWildcards : JvmSuppressWildcards -> JvmSuppressWildcards
// bar3 : [@kotlin.jvm.JvmWildcard] A<X, X> -> [@kotlin.jvm.JvmWildcard] A<in X, out X>
// - INVARIANT X : X -> X
// - INVARIANT X : X -> X
// - @JvmWildcard : JvmWildcard -> JvmWildcard
// p1 : A<in X, out X> -> A<X, X>
// - CONTRAVARIANT X : X -> X
// - COVARIANT X : X -> X
// p1.getter() : A<in X, out X> -> A<X, X>
// - CONTRAVARIANT X : X -> X
// - COVARIANT X : X -> X
// p2 : A<Any, Y> -> A<Any, Y>
// - INVARIANT Any : Any -> Any
// - INVARIANT Y : Y -> Y
// p2.getter() : A<Any, Y> -> A<Any, Y>
// - INVARIANT Any : Any -> Any
// - INVARIANT Y : Y -> Y
// p3 : A<*, *> -> A<Any?, Any?>
// p3.getter() : A<*, *> -> A<Any?, Any?>
// - STAR Any : Any? -> Any?
// - STAR Any : Any? -> Any?
// p4 : B<X> -> B<X>
// - INVARIANT X : X -> X
// p4.getter() : B<X> -> B<X>
// - INVARIANT X : X -> X
// p5 : B<in X> -> B<in X>
// - CONTRAVARIANT X : X -> X
// p5.getter() : B<in X> -> B<in X>
// - CONTRAVARIANT X : X -> X
// p6 : B<out X> -> B<out X>
// - COVARIANT X : X -> X
// p6.getter() : B<out X> -> B<out X>
// - COVARIANT X : X -> X
// p7 : B<*> -> B<out Any?>
// p7.getter() : B<*> -> B<out Any?>
// - STAR Any : Any? -> Any?
// v1 : A<X, X> -> A<in X, out X>
// - INVARIANT X : X -> X
// - INVARIANT X : X -> X
// v2 : A<Any, Y> -> A<Any, Y>
// - INVARIANT Any : Any -> Any
// - INVARIANT Y : Y -> Y
// v3 : A<*, *> -> A<out Any?, out Any?>
// v4 : B<X> -> B<X>
// - INVARIANT X : X -> X
// v5 : B<in X> -> B<in X>
// - CONTRAVARIANT X : X -> X
// v6 : B<out X> -> B<out X>
// - COVARIANT X : X -> X
// v7 : B<*> -> B<out Any?>
// foo : Unit -> Unit
// r1 : A<X, X> -> A<X, X>
// - INVARIANT X : X -> X
// - INVARIANT X : X -> X
// r2 : A<Any, Y> -> A<Any, Y>
// - INVARIANT Any : Any -> Any
// - INVARIANT Y : Y -> Y
// r3 : A<*, *> -> A<Any?, Any?>
// r4 : B<X> -> B<X>
// - INVARIANT X : X -> X
// r5 : B<in X> -> B<in X>
// - CONTRAVARIANT X : X -> X
// r6 : B<out X> -> B<out X>
// - COVARIANT X : X -> X
// r7 : B<*> -> B<out Any?>
// C1 : A<X, X> -> A<X, X>
// - INVARIANT X : X -> X
// - INVARIANT X : X -> X
// <init> : C1 -> C1
// C2 : A<Any, Y> -> A<Any, Y>
// - INVARIANT Any : Any -> Any
// - INVARIANT Y : Y -> Y
// <init> : C2 -> C2
// C3 : B<X> -> B<X>
// - INVARIANT X : X -> X
// <init> : C3 -> C3
// C4 : B<A<X, out Y>> -> B<A<in X, out Y>>
// - INVARIANT A : A<X, out Y> -> A<in X, out Y>
// - - INVARIANT X : X -> X
// - - COVARIANT Y : Y -> Y
// <init> : C4 -> C4
// a1 : A<in X, out X> -> A<in X, out X>
// - CONTRAVARIANT X : X -> X
// - COVARIANT X : X -> X
// a2 : A<Any, Y> -> A<Any, Y>
// - INVARIANT Any : Any -> Any
// - INVARIANT Y : Y -> Y
// a3 : A<*, *> -> A<out Any?, out Any?>
// a4 : B<X> -> B<X>
// - INVARIANT X : X -> X
// a5 : B<in X> -> B<in X>
// - CONTRAVARIANT X : X -> X
// a6 : B<out X> -> B<out X>
// - COVARIANT X : X -> X
// a7 : B<*> -> B<out Any?>
// <init> : A1 -> A1
// a1 : A<in X, out X> -> A<in X, out X>
// - CONTRAVARIANT X : X -> X
// - COVARIANT X : X -> X
// a1.getter() : A<in X, out X> -> A<in X, out X>
// - CONTRAVARIANT X : X -> X
// - COVARIANT X : X -> X
// a2 : A<Any, Y> -> A<Any, Y>
// - INVARIANT Any : Any -> Any
// - INVARIANT Y : Y -> Y
// a2.getter() : A<Any, Y> -> A<in Any, out Y>
// - INVARIANT Any : Any -> Any
// - INVARIANT Y : Y -> Y
// a3 : A<*, *> -> A<out Any?, out Any?>
// a3.getter() : A<*, *> -> A<out Any?, out Any?>
// - STAR Any : Any? -> Any?
// - STAR Any : Any? -> Any?
// a4 : B<X> -> B<X>
// - INVARIANT X : X -> X
// a4.getter() : B<X> -> B<X>
// - INVARIANT X : X -> X
// a5 : B<in X> -> B<in X>
// - CONTRAVARIANT X : X -> X
// a5.getter() : B<in X> -> B<in X>
// - CONTRAVARIANT X : X -> X
// a6 : B<out X> -> B<out X>
// - COVARIANT X : X -> X
// a6.getter() : B<out X> -> B<out X>
// - COVARIANT X : X -> X
// a7 : B<*> -> B<out Any?>
// a7.getter() : B<*> -> B<out Any?>
// - STAR Any : Any? -> Any?
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
