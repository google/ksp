// TEST PROCESSOR: ParameterTypeProcessor
// EXPECTED:
// <ERROR TYPE>
// String
// Int
// Int
// <ERROR TYPE>
// <ERROR TYPE>
// END

class Foo {
    var a: ErrorType
    set(value) {
        a = value
    }
    var x
        get() = "OK"
        set(v) = Unit
    var a:Int
    get() = a
    set(value) { a = value }

    fun foo(a: Int, b: NonExist, c)
}