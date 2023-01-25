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
// X : Any -> Any
// <init> : X -> X
// Y : X -> X
// <init> : Y -> Y
// A : Any -> Any
// T1 : Any? -> Any?
// T2 : Any? -> Any?
// <init> : A<T1, T2> -> A<T1, T2>
// B : Any -> Any
// T : Any? -> Any?
// synthetic constructor for B : B<*> -> B<out Any?>
// bar1 : [@kotlin.jvm.JvmSuppressWildcards] A<X, X> -> [@kotlin.jvm.JvmSuppressWildcards] A<X, X>
// - INVARIANT X : X -> X
// - INVARIANT X : X -> X
// - @JvmSuppressWildcards : JvmSuppressWildcards -> JvmSuppressWildcards
// bar2 : [@kotlin.jvm.JvmSuppressWildcards] A<X, X> -> [@kotlin.jvm.JvmSuppressWildcards] A<in X, out X>
// - INVARIANT X : X -> X
// - INVARIANT X : X -> X
// - @JvmSuppressWildcards : JvmSuppressWildcards -> JvmSuppressWildcards
// bar3 : [@kotlin.jvm.JvmWildcard] A<X, X> -> [@kotlin.jvm.JvmWildcard] A<X, X>
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
// p8 : B<A<X, out Y>> -> B<A<X, Y>>
// - INVARIANT A : A<X, out Y> -> A<X, Y>
// - - INVARIANT X : X -> X
// - - COVARIANT Y : Y -> Y
// p8.getter() : B<A<X, out Y>> -> B<A<X, Y>>
// - INVARIANT A<X, out Y> : A<X, out Y> -> A<in X, Y>
// - - INVARIANT X : X -> X
// - - COVARIANT Y : Y -> Y
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
// v8 : B<A<X, out Y>> -> B<A<X, Y>>
// - INVARIANT A : A<X, out Y> -> A<X, Y>
// - - INVARIANT X : X -> X
// - - COVARIANT Y : Y -> Y
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
// r8 : B<A<X, out Y>> -> B<A<X, Y>>
// - INVARIANT A : A<X, out Y> -> A<X, Y>
// - - INVARIANT X : X -> X
// - - COVARIANT Y : Y -> Y
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
// END

open class X()
final class Y() : X()

open class A<in T1, out T2>()
open class B<T>

// FIXME: should this annotation propagate to the return type?
// @JvmSuppressWildcards(false)
// fun bar(): A<X, X> = TODO()

// A<X, X>
fun bar1(): @JvmSuppressWildcards(true) A<X, X> = TODO()
// A<? super X, ? extends X>
fun bar2(): @JvmSuppressWildcards(false) A<X, X> = TODO()
// A<X, X>
fun bar3(): @JvmWildcard A<X, X> = TODO()

// A<X, X>
val p1: A<in X, out X> = TODO()
// A<java.lang.Object, Y>
val p2: A<Any, Y> = TODO()
// A<?, ?>
val p3: A<*, *> = TODO()
// B<X>
val p4: B<X> = TODO()
// B<? super X>
val p5: B<in X> = TODO()
// B<? extends X>
val p6: B<out X> = TODO()
// B<?>
val p7: B<*> = TODO()
// B<A<X, Y>>
val p8: B<A<X, out Y>>

// void foo(A<? super X, ? extends X>, A<java.lang.Object, Y>, A<?, ?>, B<X>, B<? super X>, B<? extends X>, B<?>, B<A<X, Y>>);
fun foo(
    v1: A<X, X>,
    v2: A<Any, Y>,
    v3: A<*, *>,
    v4: B<X>,
    v5: B<in X>,
    v6: B<out X>,
    v7: B<*>,
    v8: B<A<X, out Y>>,
): Unit = Unit

// A<X, X>
fun r1(): A<X, X> = TODO()
// A<java.lang.Object, Y>
fun r2(): A<Any, Y> = TODO()
// A<?, ?>
fun r3(): A<*, *> = TODO()
// B<X>
fun r4(): B<X> = TODO()
// B<? super X>
fun r5(): B<in X> = TODO()
// B<? extends X>
fun r6(): B<out X> = TODO()
// B<?>
fun r7(): B<*> = TODO()
// B<A<X, Y>>
fun r8(): B<A<X, out Y>> = TODO()

// extends A<X, X>
class C1() : A<X, X>()
// A<java.lang.Object, Y>
class C2() : A<Any, Y>()
// B<X>
class C3() : B<X>()
// B<A<? super X, ? extends Y>>
class C4() : B<A<X, out Y>>()
