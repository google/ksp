// WITH_RUNTIME
// TEST PROCESSOR: PlatformDeclarationProcessor
// EXPECTED:
// Actual.kt : Clazz : true : false : [] : [Expect.kt]
// Actual.kt : Clazz.foo : true : false : [] : [Expect.kt]
// Actual.kt : ExpectNotFoundClass : true : false : [] : []
// Actual.kt : ExpectNotFoundFun : true : false : [] : []
// Actual.kt : ExpectNotFoundVal : true : false : [] : []
// Actual.kt : Klass : true : false : [] : [Expect.kt]
// Actual.kt : RGB : true : false : [] : [Expect.kt]
// Actual.kt : RGB.B : true : false : [] : [Expect.kt]
// Actual.kt : RGB.G : true : false : [] : [Expect.kt]
// Actual.kt : RGB.R : true : false : [] : [Expect.kt]
// Actual.kt : RGB.v : false : false : [] : []
// Actual.kt : bar : true : false : [] : [Expect.kt]
// Actual.kt : baz : true : false : [] : [Expect.kt]
// Coffee.java : Coffee : false : false : [] : []
// Coffee.java : Coffee.baz : false : false : [] : []
// Coffee.java : Coffee.foo : false : false : [] : []
// Expect.kt : ActualNotFoundClass : false : true : [] : []
// Expect.kt : ActualNotFoundFun : false : true : [] : []
// Expect.kt : ActualNotFoundVal : false : true : [] : []
// Expect.kt : Clazz : false : true : [Actual.kt] : []
// Expect.kt : Clazz.foo : false : true : [Actual.kt] : []
// Expect.kt : Klass : false : true : [Actual.kt] : []
// Expect.kt : NormalClass : false : false : [] : []
// Expect.kt : NormalFun : false : false : [] : []
// Expect.kt : NormalVal : false : false : [] : []
// Expect.kt : RGB : false : true : [Actual.kt] : []
// Expect.kt : RGB.B : false : true : [Actual.kt] : []
// Expect.kt : RGB.G : false : true : [Actual.kt] : []
// Expect.kt : RGB.R : false : true : [Actual.kt] : []
// Expect.kt : bar : false : true : [Actual.kt] : []
// Expect.kt : baz : false : true : [Actual.kt] : []
// END

// FILE: Expect.kt
expect class Clazz {
    fun foo(): String
}

expect fun bar(): String
expect val baz: String
expect class Klass

class NormalClass
fun NormalFun(): String = ""
val NormalVal: String = ""

expect class ActualNotFoundClass
expect fun ActualNotFoundFun(): String
expect val ActualNotFoundVal: String

expect enum class RGB {
    R,
    expect G,
    B
}

// FILE: Actual.kt
actual class Clazz {
    actual fun foo(): String = "foo"
}

actual fun bar(): String = "bar"
actual val baz: String = "baz"
actual typealias Klass = String

actual class ExpectNotFoundClass
actual fun ExpectNotFoundFun(): String
actual val ExpectNotFoundVal: String

actual enum class RGB(val v: Int) {
    actual R(0xFF0000),
    actual G(0x00FF00),
    actual B(0x0000FF)
}

// FILE: Coffee.java
class Coffee {
    String foo() {
        return null
    }

    String baz = null
}
