/*
 * Copyright 2021 Google LLC
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
// TEST PROCESSOR: BackingFieldProcessor
// EXPECTED:
// lib.DataClass.value_Param: true
// lib.DataClass.variable_Param: true
// lib.JavaClass.javaField: true
// lib.JavaClass.javaFieldWithAccessors: true
// lib.NormalClass.value: true
// lib.NormalClass.value_Param: true
// lib.NormalClass.value_noBacking: false
// lib.NormalClass.value_withBacking: true
// lib.NormalClass.variable: true
// lib.NormalClass.variable_Param: true
// lib.NormalClass.variable_noBacking: false
// lib.NormalClass.variable_withBacking: true
// lib.value: true
// lib.value_noBacking: false
// lib.value_withBacking: true
// lib.variable: true
// lib.variable_noBacking: false
// lib.variable_withBacking: true
// main.DataClass.value_Param: true
// main.DataClass.variable_Param: true
// main.JavaClass.javaField: true
// main.JavaClass.javaFieldWithAccessors: true
// main.NormalClass.value: true
// main.NormalClass.value_Param: true
// main.NormalClass.value_noBacking: false
// main.NormalClass.value_withBacking: true
// main.NormalClass.variable: true
// main.NormalClass.variable_Param: true
// main.NormalClass.variable_noBacking: false
// main.NormalClass.variable_withBacking: true
// main.value: true
// main.value_noBacking: false
// main.value_withBacking: true
// main.variable: true
// main.variable_noBacking: false
// main.variable_withBacking: true
// END

// MODULE: lib
// FILE: lib.kt
package lib
val value: String = ""
var variable: String = ""
val value_noBacking: String
    get() = "aa"
var variable_noBacking: String
    get() = "aa"
    set(value) {}
val value_withBacking: String = ""
    get() = field
var variable_withBacking: String? = null
    get() = field
data class DataClass(
    val value_Param: String,
    var variable_Param: String
)

class NormalClass(
    val value_Param: String,
    var variable_Param: String,
    normalParam: String
) {
    val value: String = ""
    var variable: String = ""
    val value_noBacking: String
        get() = "aa"
    var variable_noBacking: String
        get() = "aa"
        set(value) {}
    val value_withBacking: String = ""
        get() = field
    var variable_withBacking: String? = null
        get() = field
}

// FILE: lib/JavaClass.java
package lib;
public class JavaClass {
    private String javaField;
    private String javaFieldWithAccessors;

    public String getJavaFieldWithAccessors() { return ""; }
    public void setJavaFieldWithAccessors(String value) { }

    public String getJavaAccessorWithoutField() { return ""; }
    public void setJavaAccessorWithoutField(String value) { }
}

// MODULE: main(lib)
// FILE: main.kt
package main
val value: String = ""
var variable: String = ""
val value_noBacking: String
    get() = "aa"
var variable_noBacking: String
    get() = "aa"
    set(value) {}
val value_withBacking: String = ""
    get() = field
var variable_withBacking: String? = null
    get() = field

data class DataClass(
    val value_Param: String,
    var variable_Param: String
)
class NormalClass(
    val value_Param: String,
    var variable_Param: String,
    normalParam: String
) {
    val value: String = ""
    var variable: String = ""
    val value_noBacking: String
        get() = "aa"
    var variable_noBacking: String
        get() = "aa"
        set(value) {}
    val value_withBacking: String = ""
        get() = field
    var variable_withBacking: String? = null
        get() = field
}
// FILE: main/JavaClass.java
package main;
public class JavaClass {
    private String javaField;
    private String javaFieldWithAccessors;

    public String getJavaFieldWithAccessors() { return ""; }
    public void setJavaFieldWithAccessors(String value) { }

    public String getJavaAccessorWithoutField() { return ""; }
    public void setJavaAccessorWithoutField(String value) { }
}
