// TEST PROCESSOR: DeclarationPackageNameProcessor
// EXPECTED:
// test.pack:Outer
// test.pack:Val
// test.pack:Foo
// test.pack:Inner
// test.pack:innerVal
// test.pack:innerFoo
// test.pack:InnerLocal
// test.pack:Nested
// test.pack:nestedVal
// test.pack:nestedFoo
// test.pack:a
// test.java.pack:C
// test.java.pack:Inner
// test.java.pack:Nested
// END
//FILE: a.kt
package test.pack

class Outer {
    val Val

    fun Foo() {}

    inner class Inner {
        val innerVal: Int
        fun innerFoo() {
            class InnerLocal
        }
    }
    class Nested {
        private val nestedVal: Int
        fun nestedFoo() {
            val a = 1
        }
    }
}

//FILE: C.java
package test.java.pack;

public class C {
    class Inner {

    }

    static class Nested  {}
}