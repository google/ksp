// WITH_RUNTIME
// TEST PROCESSOR: ReferenceElementProcessor
// EXPECTED:
// Qualifier of B is A
// Qualifier of C is A
// Qualifier of Int is null
// Qualifier of B is A
// Qualifier of C is A<Int>
// Qualifier of Int is null
// END

class A<T> {
    class B
    inner class C
}

val x: A.B = A.B()
val y: A<Int>.C = A<Int>().C()
