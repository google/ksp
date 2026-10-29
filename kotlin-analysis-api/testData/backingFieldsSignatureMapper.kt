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

// WITH_RUNTIME
// TEST PROCESSOR: BackingFieldsSignatureMapperProcessor
// PROCESSOR INPUT: MyClass, MyJavaClass, lib.Other
// EXPECTED:
// MyClass.implicitInt: I
// EXPECT NEXT: MyClass.implicitInt.field: I
// MyClass.explicitList: Ljava/util/List;
// EXPECT NEXT: MyClass.explicitList.field: Ljava/util/ArrayList;
// MyClass.propA: LA;
// EXPECT NEXT: MyClass.propA.field: LB;
// MyClass.noBackingField: I
// MyJavaClass.javaInt: I
// EXPECT NEXT: MyJavaClass.javaInt.field: I
// MyJavaClass.javaString: Ljava/lang/String;
// EXPECT NEXT: MyJavaClass.javaString.field: Ljava/lang/String;
// lib.Other.implicitInt: I
// EXPECT NEXT: lib.Other.implicitInt.field: I
// lib.Other.explicitList: Ljava/util/List;
// EXPECT NEXT: lib.Other.explicitList.field: Ljava/util/List;
// lib.Other.propA: Llib/A;
// EXPECT NEXT: lib.Other.propA.field: Llib/A;
// END

// MODULE: lib
// FILE: lib/Other.kt
package lib

open class A
class B : A()

class Other {
    val implicitInt: Int = 1

    val explicitList: List<String>
        field: ArrayList<String> = arrayListOf("")

    val propA: A
        field: B = B()
}

// MODULE: main(lib)
// FILE: MyClass.kt
open class A
class B : A()

class MyClass {
    val implicitInt: Int = 1

    val explicitList: List<String>
        field: ArrayList<String> = arrayListOf("")

    val propA: A
        field: B = B()

    val noBackingField: Int
        get() = 42
}

// FILE: MyJavaClass.java
public class MyJavaClass {
    public int javaInt = 1;
    public String javaString = "hello";
}
