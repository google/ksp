// TEST PROCESSOR: ImplicitElementProcessor
// EXPECTED:
// <init>; origin: SYNTHETIC
// <null>
// <null>
// readOnly.get(): SYNTHETIC
// readOnly.getter.owner: readOnly: KOTLIN
// readWrite.get(): KOTLIN
// readWrite.set(): SYNTHETIC
// END
// FILE: a.kt
class Cls {
    val readOnly: Int = 1
    var readWrite: Int = 2
    get() = 1
}

interface ITF

// FILE: JavaClass.java
public class JavaClass {
    public JavaClass() { this(1); }
    public JavaClass(int a) { this(a, "ok"); }
    public JavaClass(int a, String s) { }
}
