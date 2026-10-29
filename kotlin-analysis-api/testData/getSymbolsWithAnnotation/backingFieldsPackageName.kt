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

// TEST PROCESSOR: BackingFieldsPackageNameNPEProcessor
// PROCESSOR INPUT: annotations.Select
// EXPECTED:
// EXPECT CURRENT: annotations.Select: MyClass.myProp: test
// EXPECT CURRENT: annotations.Select: MyJavaClass.x: com.example.jcl
// EXPECT CURRENT: annotations.Select: Other.myOtherProp: my.other.test
// EXPECT NEXT: annotations.Select: MyClass.myProp.field: test
// EXPECT NEXT: annotations.Select: MyJavaClass.x.field: com.example.jcl
// EXPECT NEXT: annotations.Select: MyJavaClass.x: com.example.jcl
// EXPECT NEXT: annotations.Select: Other.myOtherProp.field: my.other.test
// END

// FILE: Select.kt
package annotations

@Target(AnnotationTarget.FIELD)
annotation class Select

// FILE: MyClass.kt
package test

import annotations.Select

class MyClass {
    @Select
    val myProp: Boolean = false
}

// FILE: Other.kt
package my.other.test

import annotations.Select

class Other {
    @field:Select
    val myOtherProp: Int = 42
}

// FILE: MyJavaClass.java
package com.example.jcl;

import annotations.Select;

public class MyJavaClass {
    @Select
    public int x = 42;
}
