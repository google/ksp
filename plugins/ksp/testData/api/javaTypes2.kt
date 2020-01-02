// WITH_RUNTIME
// TEST PROCESSOR: TypeComparisonProcessor
// EXPECTED:
// (kotlin.Array<(C..C?)>..kotlin.Array<out (C..C?)>?) ?= (kotlin.Array<(C..C?)>..kotlin.Array<out (C..C?)>?) : true
// (kotlin.Array<(C..C?)>..kotlin.Array<out (C..C?)>?) ?= Array<C> : true
// (kotlin.Array<(C..C?)>..kotlin.Array<out (C..C?)>?) ?= Array<D> : true
// Array<C> ?= (kotlin.Array<(C..C?)>..kotlin.Array<out (C..C?)>?) : true
// Array<C> ?= Array<C> : true
// Array<C> ?= Array<D> : false
// Array<D> ?= (kotlin.Array<(C..C?)>..kotlin.Array<out (C..C?)>?) : false
// Array<D> ?= Array<C> : false
// Array<D> ?= Array<D> : true
// END

// FILE: ArrayTest.java
class ArrayTest {
    public static C[] javaArrayOfC() {
        return null;
    }
}

// FILE: K.kt
@file:kotlin.Suppress("C", "D", "Suppress")

open class C
open class D : C()

val j = ArrayTest.javaArrayOfC()
val c: Array<C> = arrayOf()
val d: Array<D> = arrayOf()
