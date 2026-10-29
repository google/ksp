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
// PROCESSOR INPUT: lib.MyLibClass, com.example.MyMainClass, com.example.MyJavaClass
// EXPECTED:
// lib.MyLibClass: MyAnnotationWithDefault: [value:[]]
// lib.MyLibClass: MyAnnotationWithoutDefault: [value:[]]
// lib.MyLibClass: MyVarArgAnnotation: [value:[]]
// lib.MyLibClass: MyVarArgAnnotationPrefix: [x:42, value:[]]
// lib.MyLibClass: MyVarArgAnnotationPrefixDefault: [value:[], x:42]
// lib.MyLibClass: MyVarArgAnnotationSuffix: [x:42, value:[]]
// lib.MyLibClass: MyVarArgAnnotationSuffixDefault: [value:[], x:42]
// lib.MyLibClass: MyVarArgAnnotationPrefixSuffix: [x:42, y:true, value:[]]
// lib.MyLibClass: MyVarArgAnnotationPrefixDefaultSuffix: [y:true, value:[], x:42]
// lib.MyLibClass: MyVarArgAnnotationPrefixDefaultSuffixDefault: [value:[], x:42, y:true]
// lib.MyLibClass: MyVarArgAnnotationPrefixSuffixDefault: [x:42, value:[], y:true]
// lib.MyLibClass: MyJavaAnnotationWithDefault: [value:[]]
// lib.MyLibClass: MyJavaAnnotationWithoutDefault: [value:[]]
// lib.MyLibClass: MyJavaAnnotationPrefix: [x:42, value:[]]
// lib.MyLibClass: MyJavaAnnotationPrefixDefault: [value:[], x:42]
// lib.MyLibClass: MyJavaAnnotationSuffix: [x:42, value:[]]
// lib.MyLibClass: MyJavaAnnotationSuffixDefault: [value:[], x:42]
// lib.MyLibClass: MyJavaAnnotationPrefixSuffix: [x:42, y:true, value:[]]
// lib.MyLibClass: MyJavaAnnotationPrefixDefaultSuffix: [y:true, value:[], x:42]
// lib.MyLibClass: MyJavaAnnotationPrefixDefaultSuffixDefault: [value:[], x:42, y:true]
// lib.MyLibClass: MyJavaAnnotationPrefixSuffixDefault: [x:42, value:[], y:true]
// com.example.MyMainClass: MyAnnotationWithDefault: [value:[]]
// com.example.MyMainClass: MyAnnotationWithoutDefault: [value:[]]
// com.example.MyMainClass: MyVarArgAnnotation: [value:[]]
// com.example.MyMainClass: MyVarArgAnnotationPrefix: [x:42, value:[]]
// com.example.MyMainClass: MyVarArgAnnotationPrefixDefault: [x:42, value:[]]
// com.example.MyMainClass: MyVarArgAnnotationSuffix: [x:42, value:[]]
// com.example.MyMainClass: MyVarArgAnnotationSuffixDefault: [x:42, value:[]]
// com.example.MyMainClass: MyVarArgAnnotationPrefixSuffix: [x:42, y:true, value:[]]
// com.example.MyMainClass: MyVarArgAnnotationPrefixDefaultSuffix: [y:true, x:42, value:[]]
// com.example.MyMainClass: MyVarArgAnnotationPrefixDefaultSuffixDefault: [x:42, y:true, value:[]]
// com.example.MyMainClass: MyVarArgAnnotationPrefixSuffixDefault: [x:42, y:true, value:[]]
// com.example.MyMainClass: MyJavaAnnotationWithDefault: [value:[]]
// com.example.MyMainClass: MyJavaAnnotationWithoutDefault: [value:[]]
// com.example.MyMainClass: MyJavaAnnotationPrefix: [x:42, value:[]]
// com.example.MyMainClass: MyJavaAnnotationPrefixDefault: [x:42, value:[]]
// com.example.MyMainClass: MyJavaAnnotationSuffix: [x:42, value:[]]
// com.example.MyMainClass: MyJavaAnnotationSuffixDefault: [x:42, value:[]]
// com.example.MyMainClass: MyJavaAnnotationPrefixSuffix: [x:42, y:true, value:[]]
// com.example.MyMainClass: MyJavaAnnotationPrefixDefaultSuffix: [y:true, x:42, value:[]]
// com.example.MyMainClass: MyJavaAnnotationPrefixDefaultSuffixDefault: [x:42, y:true, value:[]]
// com.example.MyMainClass: MyJavaAnnotationPrefixSuffixDefault: [x:42, y:true, value:[]]
// com.example.MyJavaClass: JavaApplyToNull: [values:null]
// END

// MODULE: annos
// FILE: Annos.kt
package annotations

annotation class MyAnnotationWithDefault(val value: Array<String> = [])

annotation class MyAnnotationWithoutDefault(val value: Array<String>)

annotation class MyVarArgAnnotation(vararg val value: String)

annotation class MyVarArgAnnotationPrefix(val x: Int, vararg val value: String)

annotation class MyVarArgAnnotationPrefixDefault(val x: Int = 42, vararg val value: String)

annotation class MyVarArgAnnotationSuffix(vararg val value: String, val x: Int)

annotation class MyVarArgAnnotationSuffixDefault(vararg val value: String, val x: Int = 42)

annotation class MyVarArgAnnotationPrefixSuffix(val x: Int, vararg val value: String, val y: Boolean)

annotation class MyVarArgAnnotationPrefixDefaultSuffix(val x: Int = 42, vararg val value: String, val y: Boolean)

annotation class MyVarArgAnnotationPrefixDefaultSuffixDefault(val x: Int = 42, vararg val value: String, val y: Boolean = true)

annotation class MyVarArgAnnotationPrefixSuffixDefault(val x: Int, vararg val value: String, val y: Boolean = true)

// FILE: MyJavaAnnotationWithDefault.java
package annotations;

public @interface MyJavaAnnotationWithDefault {
    String[] value() default {};
}

// FILE: MyJavaAnnotationWithoutDefault.java
package annotations;

public @interface MyJavaAnnotationWithoutDefault {
    String[] value();
}

// FILE: MyJavaAnnotationPrefix.java
package annotations;

public @interface MyJavaAnnotationPrefix {
    int x();
    String[] value();
}

// FILE: MyJavaAnnotationPrefixDefault.java
package annotations;

public @interface MyJavaAnnotationPrefixDefault {
    int x() default 42;
    String[] value();
}

// FILE: MyJavaAnnotationSuffix.java
package annotations;

public @interface MyJavaAnnotationSuffix {
    String[] value();
    int x();
}

// FILE: MyJavaAnnotationSuffixDefault.java
package annotations;

public @interface MyJavaAnnotationSuffixDefault {
    String[] value();
    int x() default 42;
}

// FILE: MyJavaAnnotationPrefixSuffix.java
package annotations;

public @interface MyJavaAnnotationPrefixSuffix {
    int x();
    String[] value();
    boolean y();
}

// FILE: MyJavaAnnotationPrefixDefaultSuffix.java
package annotations;

public @interface MyJavaAnnotationPrefixDefaultSuffix {
    int x() default 42;
    String[] value();
    boolean y();
}

// FILE: MyJavaAnnotationPrefixDefaultSuffixDefault.java
package annotations;

public @interface MyJavaAnnotationPrefixDefaultSuffixDefault {
    int x() default 42;
    String[] value();
    boolean y() default true;
}

// FILE: MyJavaAnnotationPrefixSuffixDefault.java
package annotations;

public @interface MyJavaAnnotationPrefixSuffixDefault {
    int x();
    String[] value();
    boolean y() default true;
}

// FILE: JavaApplyToNull.java
package annotations;

public @interface JavaApplyToNull {
    String[] values();
}

// MODULE: lib(annos)
// FILE: MyLibClass.kt
package lib

import annotations.MyAnnotationWithDefault
import annotations.MyAnnotationWithoutDefault
import annotations.MyVarArgAnnotation
import annotations.MyVarArgAnnotationPrefix
import annotations.MyVarArgAnnotationPrefixDefault
import annotations.MyVarArgAnnotationSuffix
import annotations.MyVarArgAnnotationSuffixDefault
import annotations.MyVarArgAnnotationPrefixSuffix
import annotations.MyVarArgAnnotationPrefixDefaultSuffix
import annotations.MyVarArgAnnotationPrefixDefaultSuffixDefault
import annotations.MyVarArgAnnotationPrefixSuffixDefault
import annotations.MyJavaAnnotationWithDefault
import annotations.MyJavaAnnotationWithoutDefault
import annotations.MyJavaAnnotationPrefix
import annotations.MyJavaAnnotationPrefixDefault
import annotations.MyJavaAnnotationSuffix
import annotations.MyJavaAnnotationSuffixDefault
import annotations.MyJavaAnnotationPrefixSuffix
import annotations.MyJavaAnnotationPrefixDefaultSuffix
import annotations.MyJavaAnnotationPrefixDefaultSuffixDefault
import annotations.MyJavaAnnotationPrefixSuffixDefault

@MyAnnotationWithDefault
@MyAnnotationWithoutDefault([])
@MyVarArgAnnotation
@MyVarArgAnnotationPrefix(42)
@MyVarArgAnnotationPrefixDefault
@MyVarArgAnnotationSuffix(x = 42)
@MyVarArgAnnotationSuffixDefault
@MyVarArgAnnotationPrefixSuffix(x = 42, y = true)
@MyVarArgAnnotationPrefixDefaultSuffix(y = true)
@MyVarArgAnnotationPrefixDefaultSuffixDefault
@MyVarArgAnnotationPrefixSuffixDefault(x = 42)
@MyJavaAnnotationWithDefault
@MyJavaAnnotationWithoutDefault
@MyJavaAnnotationPrefix(x = 42)
@MyJavaAnnotationPrefixDefault
@MyJavaAnnotationSuffix(x = 42)
@MyJavaAnnotationSuffixDefault
@MyJavaAnnotationPrefixSuffix(x = 42, y = true)
@MyJavaAnnotationPrefixDefaultSuffix(y = true)
@MyJavaAnnotationPrefixDefaultSuffixDefault
@MyJavaAnnotationPrefixSuffixDefault(x = 42)
class MyLibClass

// MODULE: main(lib, annos)
// FILE: MyMainClass.kt
package com.example

import annotations.MyAnnotationWithDefault
import annotations.MyAnnotationWithoutDefault
import annotations.MyVarArgAnnotation
import annotations.MyVarArgAnnotationPrefix
import annotations.MyVarArgAnnotationPrefixDefault
import annotations.MyVarArgAnnotationSuffix
import annotations.MyVarArgAnnotationSuffixDefault
import annotations.MyVarArgAnnotationPrefixSuffix
import annotations.MyVarArgAnnotationPrefixDefaultSuffix
import annotations.MyVarArgAnnotationPrefixDefaultSuffixDefault
import annotations.MyVarArgAnnotationPrefixSuffixDefault
import annotations.MyJavaAnnotationWithDefault
import annotations.MyJavaAnnotationWithoutDefault
import annotations.MyJavaAnnotationPrefix
import annotations.MyJavaAnnotationPrefixDefault
import annotations.MyJavaAnnotationSuffix
import annotations.MyJavaAnnotationSuffixDefault
import annotations.MyJavaAnnotationPrefixSuffix
import annotations.MyJavaAnnotationPrefixDefaultSuffix
import annotations.MyJavaAnnotationPrefixDefaultSuffixDefault
import annotations.MyJavaAnnotationPrefixSuffixDefault

@MyAnnotationWithDefault
@MyAnnotationWithoutDefault([])
@MyVarArgAnnotation
@MyVarArgAnnotationPrefix(42)
@MyVarArgAnnotationPrefixDefault
@MyVarArgAnnotationSuffix(x = 42)
@MyVarArgAnnotationSuffixDefault
@MyVarArgAnnotationPrefixSuffix(x = 42, y = true)
@MyVarArgAnnotationPrefixDefaultSuffix(y = true)
@MyVarArgAnnotationPrefixDefaultSuffixDefault
@MyVarArgAnnotationPrefixSuffixDefault(x = 42)
@MyJavaAnnotationWithDefault
@MyJavaAnnotationWithoutDefault
@MyJavaAnnotationPrefix(x = 42)
@MyJavaAnnotationPrefixDefault
@MyJavaAnnotationSuffix(x = 42)
@MyJavaAnnotationSuffixDefault
@MyJavaAnnotationPrefixSuffix(x = 42, y = true)
@MyJavaAnnotationPrefixDefaultSuffix(y = true)
@MyJavaAnnotationPrefixDefaultSuffixDefault
@MyJavaAnnotationPrefixSuffixDefault(x = 42)
class MyMainClass

// FILE: MyJavaClass.java
package com.example;

import annotations.JavaApplyToNull;

@JavaApplyToNull(null)
class MyJavaClass {
}
