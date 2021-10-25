// WITH_RUNTIME
// TEST PROCESSOR: ValidateProcessor
// EXPECTED:
// ErrorInMember invalid
// goodProp valid
// badProp invalid
// errorFun invalid
// <init> valid
// SkipErrorInMember valid
// skipProp valid
// skipFun valid
// <init> valid
// GoodClass valid
// C valid
// BadJavaClass invalid
// ErrorAnnotationType invalid
// END
// FILE: a.kt
annotation class Anno(val i: Int)

@Anno(1)
class ErrorInMember : C {
    val goodProp: Int
    val badProp: () -> NonExistType
    fun errorFun(): NonExistType {

    }
}

class SkipErrorInMember {
    val skipProp: NonExistType
    fun skipFun(): NonExitType {

    }
}

@NonExistAnnotation
class ErrorAnnotationType {
}

@Anno(1)
open class GoodClass {
    val a: Int = 1

    fun foo(): Int = 1

    fun bar() {
        val x = a
    }
}

// FILE: C.java

public class C extends GoodClass {}

class BadJavaClass extends NonExistType {

}