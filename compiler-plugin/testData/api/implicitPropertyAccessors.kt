// TEST PROCESSOR: ImplicitPropertyAccessorProcessor
// EXPECTED:
// Int
// String
// <set-?>
// String
// END
// FILE: a.kt

class Foo {
    val privateGetterVal: Int
        private get

    var privateGetterVar: String
        set
        private get
}