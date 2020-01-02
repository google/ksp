// WITH_RUNTIME
// TEST PROCESSOR: ClassWithCompanionProcessor
// EXPECTED:
// Foo:false
// companion:false
// obj:false
// K:true
// END

class Foo {
    object companion {}
    object obj {}
    companion object K {}
}