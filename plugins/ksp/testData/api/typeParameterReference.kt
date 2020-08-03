// WITH_RUNTIME
// TEST PROCESSOR: TypeParameterReferenceProcessor
// EXPECTED:
// Foo.T1
// Foo.bar.T2
// foo.T3
// END

class Foo<T1> {
    inner class Bar {
        val v: T1
    }

    fun <T2> bar(p: T2) = 1
}

fun <T3> foo(p: T3) = 1
