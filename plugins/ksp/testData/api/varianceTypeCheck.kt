// WITH_RUNTIME
// TEST PROCESSOR: TypeComparisonProcessor
// EXPECTED:
// Foo<A> ?= Foo<A> : true
// Foo<A> ?= Foo<C> : false
// Foo<A> ?= Foo<in B> : false
// Foo<A> ?= Foo<out B> : false
// Foo<C> ?= Foo<A> : false
// Foo<C> ?= Foo<C> : true
// Foo<C> ?= Foo<in B> : false
// Foo<C> ?= Foo<out B> : false
// Foo<in B> ?= Foo<A> : true
// Foo<in B> ?= Foo<C> : false
// Foo<in B> ?= Foo<in B> : true
// Foo<in B> ?= Foo<out B> : false
// Foo<out B> ?= Foo<A> : false
// Foo<out B> ?= Foo<C> : true
// Foo<out B> ?= Foo<in B> : false
// Foo<out B> ?= Foo<out B> : true
// END

@file:kotlin.Suppress("A", "B", "C", "Suppress")

open class A
open class B: A()
open class C: B()

class Foo<T>

var ib: Foo<in B> = Foo<B>()
var ob: Foo<out B> = Foo<B>()

var a: Foo<A> = Foo<A>()
var c: Foo<C> = Foo<C>()

