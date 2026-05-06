/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: GetDeclarationsProcessor
// PROCESSOR INPUT: MyInterface, MyClass
// EXPECTED:
// Declaration simpleName: Foo
// Declaration qualifiedName: MyInterface.Foo (Origin: KOTLIN)
// Declaration simpleName: getFoo
// Declaration qualifiedName: MyClass.getFoo (Origin: JAVA)
// Declaration simpleName: getFoo
// Declaration qualifiedName: MyClass.getFoo (Origin: SYNTHETIC)
// Declaration simpleName: <init>
// END

// FILE: MyInterface.kt

interface MyInterface {
    val Foo: String
}

// FILE: MyClass.java

class MyClass implements MyInterface {
    @Override
    public String getFoo() {
        return ""
    }
}
