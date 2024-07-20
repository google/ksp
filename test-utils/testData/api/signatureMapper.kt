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

// TEST PROCESSOR: MapSignatureProcessor
// EXPECTED:
// LCls;
// a: I
// foo: ()Ljava/lang/String;
// f: ()I
// <init>: ()V
// LJavaIntefaceWithVoid;
// getVoid: ()Ljava/lang/Void;
// LJavaClass;
// <init>: ()V
// LJavaAnno;
// intParam: I
// <init>: (I)V
// LJavaEnum;
// VAL1: LJavaEnum;
// VAL2: LJavaEnum;
// DEFAULT: LJavaEnum;
// values: ()[LJavaEnum;
// valueOf: (Ljava/lang/String;)LJavaEnum;
// <init>: (Ljava/lang/String;I)V
// END

// FILE: Cls.kt
@JvmInline
value class MyInlineClass(val value: Int)

class Cls {
    val a: Int = 1

    fun foo(): String { return "1" }

    fun f(): MyInlineClass = 1
}

// FILE: JavaIntefaceWithVoid.java
interface JavaIntefaceWithVoid {
    Void getVoid();
}

// FILE: JavaClass.java
class JavaClass {
    JavaClass() {}
}

// FILE: JavaAnno.java
@interface JavaAnno {
    int intParam();
}

// FILE: JavaEnum.java
public enum JavaEnum {
    VAL1,
    VAL2,
    DEFAULT
}
