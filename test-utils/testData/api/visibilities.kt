/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: VisibilityProcessor
// EXPECTED:
// publicFun: PUBLIC,visible in A, B, D: true, true, true
// packageFun: JAVA_PACKAGE,visible in A, B, D: true, false, true
// privateFun: PRIVATE,visible in A, B, D: false, false, false
// protectedFun: PROTECTED,visible in A, B, D: true, false, true
// <init>: JAVA_PACKAGE,visible in A, B, D: true, false, true
// javaPackageField: JAVA_PACKAGE,visible in A, B, D: true, false, true
// x: INTERNAL,visible in A, B, D: false, false, false
// y: PROTECTED,visible in A, B, D: true, false, true
// y: PUBLIC,visible in A, B, D: true, true, true
// LibEnumJava: valueOf: PUBLIC
// LibEnumJava: values: PUBLIC
// LibEnumJava: <init>: PRIVATE
// LibEnum: valueOf: PUBLIC
// LibEnum: values: PUBLIC
// LibEnum: <init>: PRIVATE
// Enum: <init>: PRIVATE
// Enum: values: PUBLIC
// Enum: valueOf: PUBLIC
// KtEnum: <init>: PRIVATE
// KtEnumWithVal: <init>: PRIVATE
// END

// MODULE: lib
// FILE: JavaClass.java
public class JavaClass {
    int javaPackageField;
}

// FILE: LibEnumJava.java
public enum LibEnumJava {
    R(0),G(1),B(2);
    private final int v;
    LibEnumJava(int v) {
        this.v = v;
    }
}

// FILE: lib.kt
open class KotlinClass {
    open internal val x: Int = 0
    open protected val y: Int = 0
}

enum class LibEnum(val value: Int) {
    A(0), B(1), C(2);
}

// MODULE: main(lib)
// FILE: a.kt
annotation class TestA
annotation class TestB
annotation class TestD

@TestA
class A : C() {
}

@TestD
class D {}

class KotlinSubClass : KotlinClass() {
    public override val y = 1
}

enum class KtEnum {
    A,B,C
}

enum class KtEnumWithVal(val a: Int) {
    A(0), B(1), C(2)
}

// FILE: C.java
class C {
    public int publicFun() {
        return 1;
    }

    int packageFun() {
        return 1;
    }

    private int privateFun() {
        return 1;
    }

    protected int protectedFun() {
        return 1;
    }
}

// FILE: Enum.java
public enum Enum {
    Y,U,V;
    private final int v;
    Enum(int v) {
        this.v = v;
    }
}
// FILE: b.kt
package somePackage

import TestB

@TestB
class B
