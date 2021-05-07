// WITH_RUNTIME
// TEST PROCESSOR: CrossModuleTypeAliasTestProcessor
// EXPECTED:
// A1(KOTLIN)
// A2(KOTLIN_LIB)
// M1(KOTLIN_LIB)
// END

// MODULE: module1
// FILE: M1.kt
class M1

// MODULE: module2(module1)
// FILE: M2.kt
typealias A2 = M1

// MODULE: main(module1, module2)
// FILE: main.kt
typealias A1 = M1

class TestTarget {
    val a1: A1 = TODO()
    val a2: A2 = TODO()
    val m1: M1 = TODO()
}
