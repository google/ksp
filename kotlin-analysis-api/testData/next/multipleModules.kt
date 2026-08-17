/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
