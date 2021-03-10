// TEST PROCESSOR: NestedClassTypeProcessor
// EXPECTED:
// bar
// , @TypeAnno
// CONTRAVARIANT Int,COVARIANT String
// END
// FILE: a.kt
annotation class TypeAnno

class Outer<T> {
    inner class InnerGeneric<P>
    inner class Inner
}

class C {
    val bar: Outer<out @TypeAnno String>.InnerGeneric<in Int>
}