// WITH_RUNTIME
// TEST PROCESSOR: DeclarationOrderProcessor
// EXPECTED:
// ClassInMainModule[KOTLIN]
// ClassInModule1[CLASS]
// ClassInModule2[CLASS]
// JavaClassInMainModule[JAVA]
// JavaClassInModule1[CLASS]
// JavaClassInModule2[CLASS]
// TestTarget[KOTLIN]
// END
// MODULE: module1
// FILE: ClassInModule1.kt
class ClassInModule1 {
    val prop1: String = TODO()
    val prop2: String? = TODO()
    fun fun1(): String = TODO()
    fun fun2() :String = TODO()
}
// FILE: JavaClassInModule1.java
public class JavaClassInModule1 {
    String field1 = "";
    String field2 = "";
    void function1() {}
    void function2() {}
}
// MODULE: main(module1)
// FILE: main.kt
class ClassInMain {
    val prop1: String = TODO()
    val prop2: String? = TODO()
    fun fun1(): String = TODO()
    fun fun2() :String = TODO()
}
// FILE: JavaClassInModule1.java
public class JavaClassInMain {
    String field1 = "";
    String field2 = "";
    void function1() {}
    void function2() {}
}
