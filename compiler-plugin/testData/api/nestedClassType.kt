// TEST PROCESSOR: NestedClassTypeProcessor
// EXPECTED:
// foo
// @TypeAnno1
// COVARIANT String
// bar
// , @TypeAnno2
// CONTRAVARIANT Int,INVARIANT String
// END
// FILE: a.kt
annotation class TypeAnno1
annotation class TypeAnno2

class Outer<T> {
    inner class InnerGeneric<P>
    inner class Inner
}

class G<T>

class C {
    val foo: Outer<out @TypeAnno1 String>.Inner
    val bar: Outer<@TypeAnno2 String>.InnerGeneric<in Int>
}