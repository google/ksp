// WITH_RUNTIME
// TEST PROCESSOR: MultiModuleTestProcessor
// EXPECTED:
// ClassInMainModule[KOTLIN]
// ClassInModule1[KOTLIN_LIB]
// ClassInModule2[KOTLIN_LIB]
// JavaClassInMainModule[JAVA]
// JavaClassInModule1[JAVA_LIB]
// JavaClassInModule2[JAVA_LIB]
// TestTarget[KOTLIN]
// END
// MODULE: module1
// FILE: ClassInModule1.kt
class ClassInModule1 {
    val javaClassInModule1: JavaClassInModule1 = TODO()
}
// FILE: JavaClassInModule1.java
public class JavaClassInModule1 {}
// MODULE: module2(module1)
// FILE: ClassInModule2.kt
class ClassInModule2 {
    val javaClassInModule2: JavaClassInModule2 = TODO()
    val classInModule1: ClassInModule1 = TODO()
}
// FILE: JavaClassInModule2.java
public class JavaClassInModule2 {}
// MODULE: main(module1, module2)
// FILE: main.kt
class TestTarget {
    val field: ClassInMainModule = TODO()
}
// FILE: ClassInMainModule.kt
class ClassInMainModule {
    val field: ClassInModule2 = TODO()
    val javaClassInMainModule : JavaClassInMainModule = TODO()
}
// FILE: JavaClassInMainModule.java
class JavaClassInMainModule {
}
