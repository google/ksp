// TEST PROCESSOR: ImplicitElementProcessor
// EXPECTED:
// <init>; origin: SYNTHETIC
// <null>
// <null>
// readOnly.get(): SYNTHETIC annotations from property: GetAnno
// readOnly.getter.owner: readOnly: KOTLIN
// readWrite.get(): KOTLIN
// readWrite.set(): SYNTHETIC annotations from property: SetAnno
// comp1.get(): SYNTHETIC
// comp2.get(): SYNTHETIC
// comp2.set(): SYNTHETIC
// END
// FILE: a.kt
annotation class GetAnno
annotation class SetAnno

class Cls {
    @get:GetAnno
    val readOnly: Int = 1

    @set:SetAnno
    var readWrite: Int = 2
    get() = 1
}

data class Data(val comp1: Int, var comp2: Int)

interface ITF

// FILE: JavaClass.java
public class JavaClass {
    public JavaClass() { this(1); }
    public JavaClass(int a) { this(a, "ok"); }
    public JavaClass(int a, String s) { }
}
