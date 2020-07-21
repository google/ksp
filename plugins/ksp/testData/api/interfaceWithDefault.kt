// TEST PROCESSOR: DefaultFunctionProcessor
// EXPECTED:
// funLiteral: false
// funWithBody: false
// emptyFun: true
// foo: false
// bar: true
// iterator: true
// equals: false
// interfaceProperty: true
// B: true
// parameterVal: false
// abstractProperty: true
// a: false
// END
// FILE: a.kt
interface KTInterface: Sequence<String> {
    fun funLiteral() = 1

    fun funWithBody(): Int {
        return 1
    }

    fun emptyFun()

    val interfaceProperty: String

}

abstract class B(val parameterVal: String) {
    abstract val abstractProperty: String
    val a: String = "str"
}

// FILE: C.java
interface C {
    default int foo() {
        return 1;
    }

    int bar()
}