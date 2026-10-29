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

// TEST PROCESSOR: JavaBackingFieldsParentsProcessor
// PROCESSOR INPUT: annotations.Select
// EXPECTED:
// EXPECT CURRENT: annotations.Select: Parents as fqn: MyJavaClass.myJavaProperty
// EXPECT CURRENT: annotations.Select: Property qualifiedName: com.example.test.MyJavaClass.myJavaProperty
// EXPECT NEXT: annotations.Select: Parents as fqn: MyJavaClass.myJavaProperty
// EXPECT NEXT: annotations.Select: Parents as fqn: MyJavaClass.myJavaProperty.field
// EXPECT NEXT: annotations.Select: Property qualifiedName: com.example.test.MyJavaClass.myJavaProperty
// EXPECT NEXT: annotations.Select: Property qualifiedName: com.example.test.MyJavaClass.myJavaProperty.field
// END

// FILE: Anno.kt

package annotations

@Target(AnnotationTarget.FIELD)
annotation class Select

// FILE: MyJavaClass.java

package com.example.test;

import annotations.Select;

public class MyJavaClass {
    @Select
    public int myJavaProperty = 42;
}
