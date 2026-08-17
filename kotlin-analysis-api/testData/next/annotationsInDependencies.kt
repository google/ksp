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
// TEST PROCESSOR: AnnotationsInDependenciesProcessor
// EXPECTED:
// main.KotlinClass ->
// class main.KotlinClass 180 : annotations.ClassTarget{[value = onClass : 179]} : 179
// class main.KotlinClass 180 : annotations.NoTargetAnnotation{[value = onClass : 178]} : 178
// function myFun 193 : annotations.FunctionTarget{[value = onMyFun : 192]} : 192
// function myFun 193 : annotations.NoTargetAnnotation{[value = onMyFun : 191]} : 191
// getter of property prop 189 : annotations.AllTarget{[value = all: : 188]} : <no line>
// getter of property prop 189 : annotations.PropertyGetterTarget{[value = get: : 185]} : <no line>
// parameter param1 196 : annotations.NoTargetAnnotation{[value = onParam1 : 194]} : <no line>
// parameter param1 196 : annotations.ValueParameterTarget{[value = onParam1 : 195]} : <no line>
// parameter param2 199 : annotations.NoTargetAnnotation{[value = onParam2 : 197]} : <no line>
// parameter param2 199 : annotations.ValueParameterTarget{[value = onParam2 : 198]} : <no line>
// parameter value <no line> : annotations.AllTarget{[value = all: : 188]} : <no line>
// parameter value <no line> : annotations.ValueParameterTarget{[value = onProp : 187]} : <no line>
// property prop 189 : annotations.AllTarget{[value = all: : 188]} : <no line>
// property prop 189 : annotations.AllTarget{[value = all: : 188]} : <no line>
// property prop 189 : annotations.FieldTarget2{[value = field: : 186]} : <no line>
// property prop 189 : annotations.FieldTarget{[value = onProp : 182]} : <no line>
// property prop 189 : annotations.NoTargetAnnotation{[value = onProp : 181]} : <no line>
// property prop 189 : annotations.PropertyTarget{[value = onProp : 183]} : <no line>
// setter of property prop 189 : annotations.PropertySetterTarget{[value = set: : 184]} : <no line>
// lib.KotlinClass ->
// class lib.KotlinClass <no line> : annotations.ClassTarget{[value = onClass : <no line>]} : <no line>
// class lib.KotlinClass <no line> : annotations.NoTargetAnnotation{[value = onClass : <no line>]} : <no line>
// function myFun <no line> : annotations.FunctionTarget{[value = onMyFun : <no line>]} : <no line>
// function myFun <no line> : annotations.NoTargetAnnotation{[value = onMyFun : <no line>]} : <no line>
// getter of property prop <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// getter of property prop <no line> : annotations.PropertyGetterTarget{[value = get: : <no line>]} : <no line>
// parameter param1 <no line> : annotations.NoTargetAnnotation{[value = onParam1 : <no line>]} : <no line>
// parameter param1 <no line> : annotations.ValueParameterTarget{[value = onParam1 : <no line>]} : <no line>
// parameter param2 <no line> : annotations.NoTargetAnnotation{[value = onParam2 : <no line>]} : <no line>
// parameter param2 <no line> : annotations.ValueParameterTarget{[value = onParam2 : <no line>]} : <no line>
// parameter propInConstructor <no line> : annotations.ValueParameterTarget{[value = propInConstructor : <no line>]} : <no line>
// parameter value <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// property prop <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// property prop <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// property prop <no line> : annotations.FieldTarget2{[value = field: : <no line>]} : <no line>
// property prop <no line> : annotations.FieldTarget{[value = onProp : <no line>]} : <no line>
// property prop <no line> : annotations.NoTargetAnnotation{[value = onProp : <no line>]} : <no line>
// property prop <no line> : annotations.PropertyTarget{[value = onProp : <no line>]} : <no line>
// setter of property prop <no line> : annotations.PropertySetterTarget{[value = set: : <no line>]} : <no line>
// main.DataClass ->
// class main.DataClass 206 : annotations.ClassTarget{[value = onDataClass : 205]} : 205
// class main.DataClass 206 : annotations.NoTargetAnnotation{[value = onDataClass : 204]} : 204
// getter of property constructorParam 217 : annotations.AllTarget{[value = all: : 216]} : <no line>
// getter of property constructorParam 217 : annotations.PropertyGetterTarget{[value = get: : 211]} : <no line>
// parameter constructorParam 217 : annotations.AllTarget{[value = all: : 216]} : <no line>
// parameter constructorParam 217 : annotations.NoTargetAnnotation{[value = onConstructorParam : 207]} : <no line>
// parameter constructorParam 217 : annotations.ValueParameterTarget{[value = onConstructorParam : 215]} : <no line>
// parameter value <no line> : annotations.AllTarget{[value = all: : 216]} : <no line>
// parameter value <no line> : annotations.ValueParameterTarget{[value = onConstructorParam : 214]} : <no line>
// property constructorParam 217 : annotations.AllTarget{[value = all: : 216]} : <no line>
// property constructorParam 217 : annotations.AllTarget{[value = all: : 216]} : <no line>
// property constructorParam 217 : annotations.FieldTarget2{[value = field: : 212]} : <no line>
// property constructorParam 217 : annotations.FieldTarget{[value = onConstructorParam : 208]} : <no line>
// property constructorParam 217 : annotations.NoTargetAnnotation{[value = onConstructorParam : 207]} : <no line>
// property constructorParam 217 : annotations.PropertyTarget{[value = onConstructorParam : 209]} : <no line>
// property constructorParam 217 : annotations.ValueParameterAndFieldTarget{[value = valueParameterAndField : 213]} : <no line>
// setter of property constructorParam 217 : annotations.PropertySetterTarget{[value = set: : 210]} : <no line>
// lib.DataClass ->
// class lib.DataClass <no line> : annotations.ClassTarget{[value = onDataClass : <no line>]} : <no line>
// class lib.DataClass <no line> : annotations.NoTargetAnnotation{[value = onDataClass : <no line>]} : <no line>
// getter of property constructorParam <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// getter of property constructorParam <no line> : annotations.PropertyGetterTarget{[value = get: : <no line>]} : <no line>
// parameter constructorParam <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// parameter constructorParam <no line> : annotations.NoTargetAnnotation{[value = onConstructorParam : <no line>]} : <no line>
// parameter value <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// property constructorParam <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// property constructorParam <no line> : annotations.AllTarget{[value = all: : <no line>]} : <no line>
// property constructorParam <no line> : annotations.FieldTarget2{[value = field: : <no line>]} : <no line>
// property constructorParam <no line> : annotations.FieldTarget{[value = onConstructorParam : <no line>]} : <no line>
// property constructorParam <no line> : annotations.NoTargetAnnotation{[value = onConstructorParam : <no line>]} : <no line>
// property constructorParam <no line> : annotations.PropertyTarget{[value = onConstructorParam : <no line>]} : <no line>
// setter of property constructorParam <no line> : annotations.PropertySetterTarget{[value = set: : <no line>]} : <no line>
// END
// MODULE: annotations
// FILE: Annotations.kt
package annotations;
annotation class NoTargetAnnotation(val value:String)

@Target(AnnotationTarget.FIELD)
annotation class FieldTarget(val value:String)

@Target(AnnotationTarget.FIELD)
annotation class FieldTarget2(val value:String)

@Target(AnnotationTarget.PROPERTY)
annotation class PropertyTarget(val value:String)

@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class PropertySetterTarget(val value:String)

@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class PropertyGetterTarget(val value:String)

@Target(AnnotationTarget.CLASS)
annotation class ClassTarget(val value:String)

@Target(AnnotationTarget.FUNCTION)
annotation class FunctionTarget(val value:String)

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class ValueParameterTarget(val value:String)

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class ValueParameterAndFieldTarget(val value: String)

annotation class AllTarget(val value: String)

// MODULE: lib(annotations)
// FILE: ClassInLib.kt
package lib;
import annotations.*;
@NoTargetAnnotation("onClass")
@ClassTarget("onClass")
class KotlinClass(@ValueParameterTarget("propInConstructor") val propInConstructor: String ) {
    @NoTargetAnnotation("onProp")
    @FieldTarget("onProp")
    @PropertyTarget("onProp")
    @set:PropertySetterTarget("set:")
    @get:PropertyGetterTarget("get:")
    @field:FieldTarget2("field:")
    @all:AllTarget("all:")
    var prop : String = ""

    @NoTargetAnnotation("onMyFun")
    @FunctionTarget("onMyFun")
    fun myFun(
        @NoTargetAnnotation("onParam1")
        @ValueParameterTarget("onParam1")
        param1: String,
        @NoTargetAnnotation("onParam2")
        @ValueParameterTarget("onParam2")
        param2: Int
    ) {
    }
}

@NoTargetAnnotation("onDataClass")
@ClassTarget("onDataClass")
class DataClass(
    @NoTargetAnnotation("onConstructorParam")
    @FieldTarget("onConstructorParam")
    @PropertyTarget("onConstructorParam")
    @set:PropertySetterTarget("set:")
    @get:PropertyGetterTarget("get:")
    @field:FieldTarget2("field:")
    @all:AllTarget("all:")
    var constructorParam : String = ""
)
// FILE: lib/JavaClass.java
package lib;
import annotations.*;
public class JavaClass {}
// MODULE: main(lib, annotations)
// FILE: ClassInModule2.kt
package main;
import annotations.*;
@NoTargetAnnotation("onClass")
@ClassTarget("onClass")
class KotlinClass {
    @NoTargetAnnotation("onProp")
    @FieldTarget("onProp")
    @PropertyTarget("onProp")
    @set:PropertySetterTarget("set:")
    @get:PropertyGetterTarget("get:")
    @field:FieldTarget2("field:")
    @setparam:ValueParameterTarget("onProp")
    @all:AllTarget("all:")
    var prop : String = ""

    @NoTargetAnnotation("onMyFun")
    @FunctionTarget("onMyFun")
    fun myFun(
        @NoTargetAnnotation("onParam1")
        @ValueParameterTarget("onParam1")
        param1: String,
        @NoTargetAnnotation("onParam2")
        @ValueParameterTarget("onParam2")
        param2: Int
    ) {
    }
}

@NoTargetAnnotation("onDataClass")
@ClassTarget("onDataClass")
class DataClass(
    @NoTargetAnnotation("onConstructorParam")
    @FieldTarget("onConstructorParam")
    @PropertyTarget("onConstructorParam")
    @set:PropertySetterTarget("set:")
    @get:PropertyGetterTarget("get:")
    @field:FieldTarget2("field:")
    @field:ValueParameterAndFieldTarget("valueParameterAndField")
    @setparam:ValueParameterTarget("onConstructorParam")
    @ValueParameterTarget("onConstructorParam")
    @all:AllTarget("all:")
    var constructorParam : String = ""
)
// FILE: main/JavaClassInModule2.java
pakage main;
import annotations.*;
@NoTargetAnnotation
class JavaClassInMain {
}
