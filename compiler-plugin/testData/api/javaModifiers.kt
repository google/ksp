// TEST PROCESSOR: JavaModifierProcessor
// EXPECTED:
// C: PUBLIC ABSTRACT
// staticStr: PRIVATE
// s1: FINAL JAVA_TRANSIENT
// i1: PROTECTED JAVA_STATIC JAVA_VOLATILE
// intFun: JAVA_SYNCHRONIZED JAVA_DEFAULT
// foo: ABSTRACT JAVA_STRICT
// END
// FILE: a.kt
annotation class Test

@Test
class Foo : C() {

}

// FILE: C.java

public abstract class C {

    private String staticStr = "str"

    final transient String s1;

    protected static volatile int i1;

    default synchronized int intFun() {
        return 1;
    }

    abstract strictfp void foo() {}
}