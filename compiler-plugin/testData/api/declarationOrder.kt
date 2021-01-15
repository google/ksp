// WITH_RUNTIME
// TEST PROCESSOR: DeclarationOrderProcessor
// EXPECTED:
// lib.KotlinClass
// name
// lastName
// nameFun
// lastNameFun
// lib.JavaClass
// name
// lastName
// nameMethod
// lastNameMethod
// KotlinClass
// name
// lastName
// nameFun
// lastNameFun
// JavaClass
// name
// lastName
// nameMethod
// lastNameMethod
// END
// MODULE: module1
// FILE: lib/KotlinClass.kt
package lib;
class KotlinClass {
    val name: String = TODO()
    val lastName: String? = TODO()
    fun nameFun(): String = TODO()
    fun lastNameFun() :String = TODO()
}
// FILE: lib/JavaClass.java
package lib;
public class JavaClass {
    // notice the non alphabetic order, which is triggering the problem
    String name = "";
    String lastName = "";
    void nameMethod() {}
    void lastNameMethod() {}
}
// MODULE: main(module1)
// FILE: main.kt
class KotlinClass {
    val name: String = TODO()
    val lastName: String? = TODO()
    fun nameFun(): String = TODO()
    fun lastNameFun() :String = TODO()
}
// FILE: JavaClass.java
public class JavaClass {
    String name = "";
    String lastName = "";
    void nameMethod() {}
    void lastNameMethod() {}
}
