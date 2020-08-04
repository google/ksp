// TEST PROCESSOR: ImplicitElementProcessor
// EXPECTED:
// <init>
// <null>
// <null>
// END
// FILE: a.kt
class Cls {
}

interface ITF

// FILE: JavaClass.java
public class JavaClass {
    public JavaClass() { this(1); }
    public JavaClass(int a) { this(a, "ok"); }
    public JavaClass(int a, String s) { }
}
