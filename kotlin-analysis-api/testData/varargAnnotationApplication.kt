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

// TEST PROCESSOR: VarArgAnnotationApplicationProcessor
// PROCESSOR INPUT: lib.MyLibClass, com.example.MyMainClass
// EXPECTED:
// lib.MyLibClass: MyAnnotationWithDefault: args = arr:[], value = []
// lib.MyLibClass: MyAnnotationWithoutDefault: args = arr:[], value = []
// lib.MyLibClass: MyVarArgAnnotation: args = values:[], value = []
// lib.MyLibClass: MyJavaAnnotationWithDefault: args = arr:[], value = []
// lib.MyLibClass: MyJavaAnnotationWithoutDefault: args = arr:[], value = []
// com.example.MyMainClass: MyAnnotationWithDefault: args = arr:[], value = []
// com.example.MyMainClass: MyAnnotationWithoutDefault: args = null, value = null
// com.example.MyMainClass: MyVarArgAnnotation: args = values:[], value = []
// com.example.MyMainClass: MyJavaAnnotationWithDefault: args = arr:[], value = []
// com.example.MyMainClass: MyJavaAnnotationWithoutDefault: args = arr:[] value = []
// END

// MODULE: annos
// FILE: Annos.kt
package annotations

annotation class MyAnnotationWithDefault(val arr: Array<String> = [])

annotation class MyAnnotationWithoutDefault(val arr: Array<String>)

annotation class MyVarArgAnnotation(vararg val values: String)

// FILE: MyJavaAnnotationWithDefault.java
package annotations;

public @interface MyJavaAnnotationWithDefault {
    String[] arr() default {};
}

// FILE: MyJavaAnnotationWithoutDefault.java
package annotations;

public @interface MyJavaAnnotationWithoutDefault {
    String[] arr();
}

// MODULE: lib(annos)
// FILE: MyLibClass.kt
package lib

import annotations.MyAnnotationWithDefault
import annotations.MyAnnotationWithoutDefault
import annotations.MyVarArgAnnotation
import annotations.MyJavaAnnotationWithDefault
import annotations.MyJavaAnnotationWithoutDefault

@MyAnnotationWithDefault([])
@MyAnnotationWithoutDefault([])
@MyVarArgAnnotation
@MyJavaAnnotationWithDefault(arr = [])
@MyJavaAnnotationWithoutDefault(arr = [])
class MyLibClass

// MODULE: main(lib, annos)
// FILE: MyMainClass.kt
package com.example

import annotations.MyAnnotationWithDefault
import annotations.MyAnnotationWithoutDefault
import annotations.MyVarArgAnnotation
import annotations.MyJavaAnnotationWithDefault
import annotations.MyJavaAnnotationWithoutDefault

@MyAnnotationWithDefault
@MyAnnotationWithoutDefault
@MyVarArgAnnotation
@MyJavaAnnotationWithDefault
@MyJavaAnnotationWithoutDefault
class MyMainClass
