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

// WITH_RUNTIME
// TEST PROCESSOR: TopLevelMemberProcessor
// EXPECTED:
// lib : <init> -> lib.LibJavaClass
// lib : <init> -> lib.RealLibClass
// lib : <init> -> lib.RealLibClass$Companion
// lib : functionInLib -> lib.LibKt
// lib : functionInLibCompanion -> lib.RealLibClass$Companion
// lib : functionInLibJvmName -> lib.LibCustomClassName
// lib : functionInLibRealClass -> lib.RealLibClass
// lib : javaFieldInLib -> lib.LibJavaClass
// lib : javaMethodInLib -> lib.LibJavaClass
// lib : jvmStaticFunctionInLibCompanion -> lib.RealLibClass$Companion
// lib : jvmStaticValueInLibCompanion -> lib.RealLibClass$Companion
// lib : jvmStaticVariableInLibCompanion -> lib.RealLibClass$Companion
// lib : valueInLib -> lib.LibKt
// lib : valueInLibCompanion -> lib.RealLibClass$Companion
// lib : valueInLibJvmName -> lib.LibCustomClassName
// lib : valueInLibRealClass -> lib.RealLibClass
// lib : variableInLib -> lib.LibKt
// lib : variableInLibCompanion -> lib.RealLibClass$Companion
// lib : variableInLibJvmName -> lib.LibCustomClassName
// lib : variableInLibRealClass -> lib.RealLibClass
// main : <init> -> main.MainJavaClass
// main : <init> -> main.RealMainClass
// main : <init> -> main.RealMainClass$Companion
// main : functionInMain -> main.MainKt
// main : functionInMainCompanion -> main.RealMainClass$Companion
// main : functionInMainJvmName -> main.MainCustomClassName
// main : functionInMainRealClass -> main.RealMainClass
// main : javaFieldInMain -> main.MainJavaClass
// main : javaMethodInMain -> main.MainJavaClass
// main : jvmStaticFunctionInMainCompanion -> main.RealMainClass$Companion
// main : jvmStaticValueInMainCompanion -> main.RealMainClass$Companion
// main : jvmStaticVariableInMainCompanion -> main.RealMainClass$Companion
// main : valueInMain -> main.MainKt
// main : valueInMainCompanion -> main.RealMainClass$Companion
// main : valueInMainJvmName -> main.MainCustomClassName
// main : valueInMainRealClass -> main.RealMainClass
// main : variableInMain -> main.MainKt
// main : variableInMainCompanion -> main.RealMainClass$Companion
// main : variableInMainJvmName -> main.MainCustomClassName
// main : variableInMainRealClass -> main.RealMainClass
// END

// MODULE: lib
// FILE: lib.kt
package lib
fun functionInLib() {}
val valueInLib: String = ""
var variableInLib: String = ""
class RealLibClass {
    fun functionInLibRealClass() {}
    val valueInLibRealClass: String = ""
    var variableInLibRealClass: String = ""

    companion object {
        fun functionInLibCompanion() {}
        val valueInLibCompanion: String = ""
        var variableInLibCompanion: String = ""
        @JvmStatic
        fun jvmStaticFunctionInLibCompanion() {}
        @JvmStatic
        val jvmStaticValueInLibCompanion: String = ""
        @JvmStatic
        var jvmStaticVariableInLibCompanion: String = ""
    }
}
// FILE: customName.kt
@file:JvmName("LibCustomClassName")
package lib
fun functionInLibJvmName() {}
val valueInLibJvmName: String = ""
var variableInLibJvmName: String = ""

// FILE: lib/LibJavaClass.java
package lib;
public class LibJavaClass {
    public LibJavaClass() {}
    private String javaFieldInLib;
    private void javaMethodInLib() {
    }
}

// MODULE: main(lib)
// FILE: main.kt
package main
fun functionInMain() {}
val valueInMain: String = ""
var variableInMain: String = ""
class RealMainClass {
    fun functionInMainRealClass() {}
    val valueInMainRealClass: String = ""
    var variableInMainRealClass: String = ""

    companion object {
        fun functionInMainCompanion() {}
        val valueInMainCompanion: String = ""
        var variableInMainCompanion: String = ""
        @JvmStatic
        fun jvmStaticFunctionInMainCompanion() {}
        @JvmStatic
        val jvmStaticValueInMainCompanion: String = ""
        @JvmStatic
        var jvmStaticVariableInMainCompanion: String = ""
    }
}
// FILE: customName.kt
@file:JvmName("MainCustomClassName")
package main
fun functionInMainJvmName() {}
val valueInMainJvmName: String = ""
var variableInMainJvmName: String = ""
// FILE: main/MainJavaClass.java
package main;
public class MainJavaClass {
    public MainJavaClass() {}
    private String javaFieldInMain;
    private void javaMethodInMain() {
    }
}
