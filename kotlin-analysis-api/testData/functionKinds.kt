/*
 * Copyright 2025 Google LLC
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// TEST PROCESSOR: FunctionKindProcessor
// EXPECTED:
// JavaClass.<init>: MEMBER
// JavaClass.javaMethod: MEMBER
// JavaClass.javaStaticMethod: STATIC
// JavaInterface.methodInInterface: MEMBER
// JavaInterface.staticMethodInInterface: STATIC
// MyClass.<init>: MEMBER
// MyClass.invoke: MEMBER
// MyClass.method: MEMBER
// MyClass.suspendMethod: MEMBER
// MyInterface.method: MEMBER
// MyInterface.methodWithImpl: MEMBER
// topLevelMethod: TOP_LEVEL
// topLevelSuspendMethod: TOP_LEVEL
// END

// FILE: K.kt
// Top-level function
fun topLevelMethod() {}

// Top-level suspend function
suspend fun topLevelSuspendMethod() {}

// Class with member function and constructor
class MyClass {
    // Member function
    fun method() {}
    suspend fun suspendMethod() {}
    operator fun invoke() {}
}

interface MyInterface {
    fun method()
    fun methodWithImpl() {}
}

// FILE: JavaClass.java
class JavaClass {
    public void javaMethod() {}
    public static void javaStaticMethod() {}
}

// FILE: JavaInterface.java
interface JavaInterface {
    void methodInInterface();
    static void staticMethodInInterface() {}
}
