// WITH_RUNTIME
// TEST PROCESSOR: ValidateProcessor
// EXPECTED:
// ErrorInMember invalid
// goodProp valid
// errorFun invalid
// GoodClass valid
// C valid
// BadJavaClass invalid
// END
// FILE: a.kt
class ErrorInMember : C {
    val goodProp: Int
    fun errorFun(): NonExistType {

    }
}

open class GoodClass {
    val a: Int

    fun foo(): Int = 1
}

// FILE: C.java

public class C extends GoodClass {}

class BadJavaClass extends NonExistType {

}