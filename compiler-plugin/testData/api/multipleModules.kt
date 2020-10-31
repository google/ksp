// WITH_RUNTIME
// TEST PROCESSOR: MultiModuleTestProcessor
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