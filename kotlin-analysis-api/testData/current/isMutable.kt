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
// TEST PROCESSOR: IsMutableProcessor
// EXPECTED:
// lib.BaseClass.abstractProp_willBeBacked: false
// lib.BaseClass.abstractProp_wontBeBacked: false
// lib.BaseClass.lateinit_var_1: true
// lib.BaseClass.lateinit_var_2: true
// lib.BaseClass.notOverriddenAbstractProp: false
// lib.BaseClass.overriddenBaseProp_willBeBacked: false
// lib.BaseClass.overriddenBaseProp_wontBeBacked: false
// lib.ChildClass.abstractProp_willBeBacked: false
// lib.ChildClass.abstractProp_wontBeBacked: false
// lib.ChildClass.interfaceProp_willBeBacked: false
// lib.ChildClass.interfaceProp_wontBeBacked: false
// lib.ChildClass.lateinit_var_1: true
// lib.ChildClass.lateinit_var_2: true
// lib.ChildClass.lateinit_var_3: true
// lib.ChildClass.overriddenBaseProp_willBeBacked: false
// lib.ChildClass.overriddenBaseProp_wontBeBacked: false
// lib.ConstructorSetProp.propSetInConstructor: false
// lib.DataClass.value_Param: false
// lib.DataClass.variable_Param: true
// lib.JavaClass.javaField: true
// lib.JavaClass.javaFieldWithAccessors: true
// lib.JavaClass.publicJavaField: true
// lib.MyInterface.interfaceProp_willBeBacked: false
// lib.MyInterface.interfaceProp_wontBeBacked: false
// lib.MyInterface.lateinit_var_3: true
// lib.NormalClass.Companion.companionVar: true
// lib.NormalClass.Companion.companion_noBackingVal: false
// lib.NormalClass.Companion.companion_noBackingVar: true
// lib.NormalClass.Companion.companion_withBackingAndGetter: false
// lib.NormalClass.Companion.staticVar: true
// lib.NormalClass.Companion.static_noBackingVal: false
// lib.NormalClass.Companion.static_noBackingVar: true
// lib.NormalClass.Companion.static_withBackingAndGetter: false
// lib.NormalClass.jvmField_withBacking: false
// lib.NormalClass.lateinit_var: true
// lib.NormalClass.value: false
// lib.NormalClass.value_Param: false
// lib.NormalClass.value_noBacking: false
// lib.NormalClass.value_withBacking: false
// lib.NormalClass.variable: true
// lib.NormalClass.variable_Param: true
// lib.NormalClass.variable_noBacking: true
// lib.NormalClass.variable_withBacking: true
// lib.lateinit_var: true
// lib.value: false
// lib.value_noBacking: false
// lib.value_withBacking: false
// lib.variable: true
// lib.variable_noBacking: true
// lib.variable_withBacking: true
// main.BaseClass.abstractProp_willBeBacked: false
// main.BaseClass.abstractProp_wontBeBacked: false
// main.BaseClass.lateinit_var_1: true
// main.BaseClass.lateinit_var_2: true
// main.BaseClass.notOverriddenAbstractProp: false
// main.BaseClass.overriddenBaseProp_willBeBacked: false
// main.BaseClass.overriddenBaseProp_wontBeBacked: false
// main.ChildClass.abstractProp_willBeBacked: false
// main.ChildClass.abstractProp_wontBeBacked: false
// main.ChildClass.interfaceProp_willBeBacked: false
// main.ChildClass.interfaceProp_wontBeBacked: false
// main.ChildClass.lateinit_var_1: true
// main.ChildClass.lateinit_var_2: true
// main.ChildClass.lateinit_var_3: true
// main.ChildClass.overriddenBaseProp_willBeBacked: false
// main.ChildClass.overriddenBaseProp_wontBeBacked: false
// main.ConstructorSetProp.propSetInConstructor: false
// main.DataClass.value_Param: false
// main.DataClass.variable_Param: true
// main.JavaClass.javaField: true
// main.JavaClass.javaFieldWithAccessors: true
// main.JavaClass.publicJavaField: true
// main.MyInterface.interfaceProp_willBeBacked: false
// main.MyInterface.interfaceProp_wontBeBacked: false
// main.MyInterface.lateinit_var_3: true
// main.NormalClass.Companion.companionVar: true
// main.NormalClass.Companion.companion_noBackingVal: false
// main.NormalClass.Companion.companion_noBackingVar: true
// main.NormalClass.Companion.companion_withBackingAndGetter: false
// main.NormalClass.Companion.staticVar: true
// main.NormalClass.Companion.static_noBackingVal: false
// main.NormalClass.Companion.static_noBackingVar: true
// main.NormalClass.Companion.static_withBackingAndGetter: false
// main.NormalClass.lateinit_var: true
// main.NormalClass.value: false
// main.NormalClass.value_Param: false
// main.NormalClass.value_noBacking: false
// main.NormalClass.value_withBacking: false
// main.NormalClass.variable: true
// main.NormalClass.variable_Param: true
// main.NormalClass.variable_noBacking: true
// main.NormalClass.variable_withBacking: true
// main.lateinit_var: true
// main.value: false
// main.value_noBacking: false
// main.value_withBacking: false
// main.variable: true
// main.variable_noBacking: true
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
lateinit var lateinit_var: String

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
    val jvmField_withBacking: String = ""
    lateinit var lateinit_var: String

    companion object {
        @JvmStatic
        var staticVar: String = ""
        @JvmStatic
        val static_withBackingAndGetter: String = ""
            get() = field
        @JvmStatic
        val static_noBackingVal: String
            get() = ""
        @JvmStatic
        var static_noBackingVar: String
            get() = ""
            set(value) {}
        var companionVar: String = ""
        val companion_withBackingAndGetter: String = ""
            get() = field
        @JvmStatic
        val companion_noBackingVal: String
            get() = ""
        @JvmStatic
        var companion_noBackingVar: String
            get() = ""
            set(value) {}
    }
}

abstract class BaseClass {
    open val overriddenBaseProp_willBeBacked: String = ""
    open val overriddenBaseProp_wontBeBacked: String = ""
    open val notOverriddenAbstractProp: String = ""
    abstract val abstractProp_willBeBacked: String
    abstract val abstractProp_wontBeBacked: String
    open lateinit var lateinit_var_1: String
    open lateinit var lateinit_var_2: String
}

interface MyInterface {
    val interfaceProp_willBeBacked: String
    val interfaceProp_wontBeBacked: String
    var lateinit_var_3: String
}

class ChildClass: BaseClass(), MyInterface {
    override val overriddenBaseProp_willBeBacked: String = ""
    override val overriddenBaseProp_wontBeBacked: String
        get() = ""
    override val abstractProp_willBeBacked: String = ""
    override val abstractProp_wontBeBacked: String
        get() = ""
    override val interfaceProp_willBeBacked: String = ""
    override val interfaceProp_wontBeBacked: String
        get() = ""
    override var lateinit_var_1: String
        get() = ""
        set(v: String) = Unit
    override var lateinit_var_2: String = ""
    override lateinit var lateinit_var_3: String
}

class ConstructorSetProp {
    private val propSetInConstructor: Boolean
    constructor(propSetInConstructor: Boolean) {
        this.propSetInConstructor = propSetInConstructor
    }
}

// FILE: lib/JavaClass.java
package lib;
public class JavaClass {
    private String javaField;
    private String javaFieldWithAccessors;
    public String publicJavaField;

    public String getJavaFieldWithAccessors()
    { return ""; }
    public void setJavaFieldWithAccessors(String value )
    {}

    public String getJavaAccessorWithoutField()
    { return ""; }
    public void setJavaAccessorWithoutField(String value )
    {}
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
lateinit var lateinit_var: String

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
    lateinit var lateinit_var: String

    companion object {
        @JvmStatic
        var staticVar: String = ""
        @JvmStatic
        val static_withBackingAndGetter: String = ""
            get() = field
        @JvmStatic
        val static_noBackingVal: String
            get() = ""
        @JvmStatic
        var static_noBackingVar: String
            get() = ""
            set(value) {}
        var companionVar: String = ""
        val companion_withBackingAndGetter: String = ""
            get() = field
        @JvmStatic
        val companion_noBackingVal: String
            get() = ""
        @JvmStatic
        var companion_noBackingVar: String
            get() = ""
            set(value) {}
    }
}

abstract class BaseClass {
    open val overriddenBaseProp_willBeBacked: String = ""
    open val overriddenBaseProp_wontBeBacked: String = ""
    open val notOverriddenAbstractProp: String = ""
    abstract val abstractProp_willBeBacked: String
    abstract val abstractProp_wontBeBacked: String
    open lateinit var lateinit_var_1: String
    open lateinit var lateinit_var_2: String
}

interface MyInterface {
    val interfaceProp_willBeBacked: String
    val interfaceProp_wontBeBacked: String
    var lateinit_var_3: String
}

class ChildClass: BaseClass(), MyInterface {
    override val overriddenBaseProp_willBeBacked: String = ""
    override val overriddenBaseProp_wontBeBacked: String
        get() = ""
    override val abstractProp_willBeBacked: String = ""
    override val abstractProp_wontBeBacked: String
        get() = ""
    override val interfaceProp_willBeBacked: String = ""
    override val interfaceProp_wontBeBacked: String
        get() = ""
    override var lateinit_var_1: String
        get() = ""
        set(v: String) = Unit
    override var lateinit_var_2: String = ""
    override lateinit var lateinit_var_3: String
}

class ConstructorSetProp {
    private val propSetInConstructor: Boolean
    constructor(propSetInConstructor: Boolean) {
        this.propSetInConstructor = propSetInConstructor
    }
}

// FILE: main/JavaClass.java
package main;
public class JavaClass {
    private String javaField;
    private String javaFieldWithAccessors;
    public String publicJavaField;

    public String getJavaFieldWithAccessors()
    { return ""; }
    public void setJavaFieldWithAccessors(String value )
    {}

    public String getJavaAccessorWithoutField()
    { return ""; }
    public void setJavaAccessorWithoutField(String value )
    {}
}
