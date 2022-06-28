// WITH_RUNTIME
// TEST PROCESSOR: LateinitPropertiesProcessor
// EXPECTED:
// prop1
// prop2
// prop3
// propSource1
// propSource2
// propSource3
// END
// MODULE: lib
// FILE: compiledProperties.kt
package test.compiled

open class Foo {
    lateinit var prop1: Any
    companion object {
        lateinit var prop2: Any
    }
}

object Bar : Foo() {
    lateinit var prop3: Any
}

// MODULE: main(lib)
// FILE: sourceProperties.kt
package test.source

open class FooSource {
    lateinit var propSource1: Any
    companion object {
        lateinit var propSource2: Any
    }
}

object BarSource : Foo() {
    lateinit var propSource3: Any
}
