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

// TEST PROCESSOR: GetSymbolsWithAnnotationProcessor
// PROCESSOR INPUT: KtEmptyTargets, KtDefaultTargets, KtClass, KtAnnoClass, KtClassAnnoClass, KtTypeParam, KtProperty, KtField, KtPropertyField, KtLocalVar, KtValueParam, KtValueParamProperty, KtValueParamField, KtValueParamPropertyField, KtConstructor, KtFunction, KtGetter, KtSetter, KtPropertyGetter, KtPropertySetter, KtGetterSetter, KtPropertyGetterSetter, KtTypeAlias, KtFile, JavaEmptyTargets, JavaDefaultTargets, JavaType, JavaAnnoType, JavaTypeAnnoType, JavaTypeParam, JavaField, JavaMethod, JavaFieldMethod, JavaLocalVar, JavaParam, JavaParamField, JavaParamMethod, JavaParamFieldMethod, JavaConstructor
// EXPECTED:
// JavaAnnoType: JavaAnnoTarget
// JavaAnnoType: KtAnnoClassTarget
// JavaConstructor: JavaMain.<init>
// JavaConstructor: KtConstructorTarget.<init>
// JavaDefaultTargets: DefaultTargetClass
// JavaDefaultTargets: JavaDefaultTargetClass
// JavaField: JavaMain.javaFieldTarget
// JavaField: KtConstructorPropertyTargets.f
// JavaField: ktFieldTarget
// JavaFieldMethod: KtConstructorPropertyTargets.pf
// JavaFieldMethod: ktPropertyFieldTarget
// JavaMethod: JavaMain.javaMethodTarget
// JavaMethod: KtConstructorPropertyTargets.p
// JavaMethod: ktFunctionTarget
// JavaMethod: ktPropertyTarget
// JavaParam: JavaMain.javaMethodTarget.p
// JavaParam: KtConstructorPropertyTargets.<init>.vp
// JavaParam: ktParamTarget.p
// JavaParamField: KtConstructorPropertyTargets.<init>.vpf
// JavaParamField: KtConstructorPropertyTargets.vpf
// JavaParamFieldMethod: KtConstructorPropertyTargets.<init>.vppf
// JavaParamFieldMethod: KtConstructorPropertyTargets.vppf
// JavaParamMethod: KtConstructorPropertyTargets.<init>.vpp
// JavaType: JavaMain
// JavaType: KtClassTarget
// JavaTypeAnnoType: JavaClassAnnoTarget
// JavaTypeAnnoType: KtClassAnnoClassTarget
// JavaTypeParam: JavaMain.T
// JavaTypeParam: JavaMain.T
// JavaTypeParam: ktTypeParamTarget.T
// KtAnnoClass: JavaAnnoTarget
// KtAnnoClass: KtAnnoClassTarget
// KtClass: JavaMain
// KtClass: KtClassTarget
// KtClassAnnoClass: JavaClassAnnoTarget
// KtClassAnnoClass: KtClassAnnoClassTarget
// KtConstructor: JavaMain.<init>
// KtConstructor: KtConstructorTarget.<init>
// KtDefaultTargets: DefaultTargetClass
// KtDefaultTargets: JavaDefaultTargetClass
// KtEmptyTargets: EmptyTargetClass
// KtEmptyTargets: JavaEmptyTargetClass
// KtField: JavaMain.javaFieldTarget
// KtField: KtConstructorPropertyTargets.f
// KtField: ktFieldTarget
// KtFile: File: Main.kt
// KtFunction: JavaMain.javaMethodTarget
// KtFunction: ktFunctionTarget
// KtGetter: ktGetterTarget.ktGetterTarget.getter()
// KtGetterSetter: ktGetterSetterTarget
// KtProperty: KtConstructorPropertyTargets.p
// KtProperty: ktPropertyTarget
// KtPropertyField: KtConstructorPropertyTargets.pf
// KtPropertyField: ktPropertyFieldTarget
// KtPropertyGetter: ktPropertyGetterTarget
// KtPropertyGetterSetter: ktPropertyGetterSetterTarget
// KtPropertySetter: ktPropertySetterTarget
// KtSetter: ktSetterTarget.ktSetterTarget.setter()
// KtTypeAlias: KtTypeAliasTarget
// KtTypeParam: JavaMain.T
// KtTypeParam: JavaMain.T
// KtTypeParam: ktTypeParamTarget.T
// KtValueParam: JavaMain.javaMethodTarget.p
// KtValueParam: KtConstructorPropertyTargets.<init>.vp
// KtValueParam: ktParamTarget.p
// KtValueParamField: KtConstructorPropertyTargets.<init>.vpf
// KtValueParamField: KtConstructorPropertyTargets.vpf
// KtValueParamProperty: KtConstructorPropertyTargets.<init>.vpp
// KtValueParamProperty: KtConstructorPropertyTargets.vpp
// KtValueParamPropertyField: KtConstructorPropertyTargets.<init>.vppf
// KtValueParamPropertyField: KtConstructorPropertyTargets.vppf
// END

// TODO: Check that every annotation is used.
// TODO: Check that every annotation is supplied in processor input comment.
// TODO: Check that every target is used.
// TODO: Check that every annotation occurs in at least one invalid use.
// TODO: Duplicate for libraries.

// FILE: KtAnnotations.kt
import kotlin.annotation.AnnotationTarget.{
    CLASS,
    ANNOTATION_CLASS,
    TYPE_PARAMETER,
    PROPERTY,
    FIELD,
    LOCAL_VARIABLE,
    VALUE_PARAMETER,
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY_GETTER,
    PROPERTY_SETTER,
    TYPE,
    EXPRESSION,
    FILE,
    TYPEALIAS
}

@Target() annotation class KtEmptyTargets
annotation class KtDefaultTargets
@Target(CLASS) annotation class KtClass
@Target(ANNOTATION_CLASS) annotation class KtAnnoClass
@Target(CLASS, ANNOTATION_CLASS) annotation class KtClassAnnoClass
@Target(TYPE_PARAMETER) annotation class KtTypeParam
@Target(PROPERTY) annotation class KtProperty
@Target(FIELD) annotation class KtField
@Target(PROPERTY, FIELD) annotation class KtPropertyField
@Target(LOCAL_VARIABLE) annotation class KtLocalVar
@Target(VALUE_PARAMETER) annotation class KtValueParam
@Target(VALUE_PARAMETER, PROPERTY) annotation class KtValueParamProperty
@Target(VALUE_PARAMETER, FIELD) annotation class KtValueParamField
@Target(VALUE_PARAMETER, PROPERTY, FIELD) annotation class KtValueParamPropertyField
@Target(CONSTRUCTOR) annotation class KtConstructor
@Target(FUNCTION) annotation class KtFunction
@Target(PROPERTY_GETTER) annotation class KtGetter
@Target(PROPERTY_SETTER) annotation class KtSetter
@Target(PROPERTY, PROPERTY_GETTER) annotation class KtPropertyGetter
@Target(PROPERTY, PROPERTY_SETTER) annotation class KtPropertySetter
@Target(PROPERTY_GETTER, PROPERTY_SETTER) annotation class KtGetterSetter
@Target(PROPERTY, PROPERTY_GETTER, PROPERTY_SETTER) annotation class KtPropertyGetterSetter
@Target(TYPE) annotation class KtType
@Target(EXPRESSION) annotation class KtExpression
@Target(TYPEALIAS) annotation class KtTypeAlias
@Target(FILE) annotation class KtFile

// FILE: JavaAnnotations.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({}) @interface JavaEmptyTargets {}
@interface JavaDefaultTargets {}
@Target(ElementType.TYPE) @interface JavaType {}
@Target(ElementType.ANNOTATION_TYPE) @interface JavaAnnoType {}
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE}) @interface JavaTypeAnnoType {}
@Target(ElementType.TYPE_PARAMETER) @interface JavaTypeParam {}
@Target(ElementType.FIELD) @interface JavaField {}
@Target(ElementType.METHOD) @interface JavaMethod {}
@Target({ElementType.FIELD, ElementType.METHOD}) @interface JavaFieldMethod {}
@Target(ElementType.LOCAL_VARIABLE) @interface JavaLocalVar {}
@Target(ElementType.PARAMETER) @interface JavaParam {}
@Target({ElementType.PARAMETER, ElementType.FIELD}) @interface JavaParamField {}
@Target({ElementType.PARAMETER, ElementType.METHOD}) @interface JavaParamMethod {}
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD}) @interface JavaParamFieldMethod {}
@Target(ElementType.CONSTRUCTOR) @interface JavaConstructor {}

// FILE: Main.kt
@file:KtFile
@file:KtDefaultTargets
@file:JavaDefaultTargets

@KtAnnoClass
@KtFunction
@JavaMethod
@JavaFieldMethod
@JavaParamMethod
@JavaParamFieldMethod
class InvalidClassTargets

@KtEmptyTargets
@KtClass
@KtClassAnnoClass
@KtTypeParam
@KtProperty
@KtField
@KtPropertyField
@KtLocalVar
@KtValueParam
@KtValueParamProperty
@KtValueParamField
@KtValueParamPropertyField
@KtConstructor
@KtGetter
@KtSetter
@KtPropertyGetter
@KtPropertySetter
@KtGetterSetter
@KtPropertyGetterSetter
@KtTypeAlias
@KtFile
@KtType
@JavaEmptyTargets
@JavaType
@JavaAnnoType
@JavaTypeAnnoType
@JavaTypeParam
@JavaField
@JavaLocalVar
@JavaParam
@JavaParamField
@JavaConstructor
fun invalidFunctionTargets() {}

@KtEmptyTargets
@JavaEmptyTargets
class EmptyTargetClass

@KtDefaultTargets
@JavaDefaultTargets
class DefaultTargetClass

@KtClass
@JavaType
class KtClassTarget

@KtAnnoClass
@JavaAnnoType
annotation class KtAnnoClassTarget

@KtClassAnnoClass
@JavaTypeAnnoType
annotation class KtClassAnnoClassTarget

@KtFunction
@JavaMethod
fun ktFunctionTarget() {}

class KtConstructorTarget
@KtConstructor
@JavaConstructor
constructor()

fun <@KtTypeParam @JavaTypeParam T> ktTypeParamTarget() {}

@KtTypeAlias
typealias KtTypeAliasTarget = String

fun ktParamTarget(
    @KtValueParam
    @JavaParam
    p: Int
) {}

fun ktLocalVarTarget() {
    @KtLocalVar
    @JavaLocalVar
    val l = 42
}

@KtProperty
@JavaMethod
val ktPropertyTarget: Int = 1

@KtField
@JavaField
val ktFieldTarget: Int = 2

@KtPropertyField
@JavaFieldMethod
val ktPropertyFieldTarget: Int = 3

val ktGetterTarget: Int
    @KtGetter get() = 1

var ktSetterTarget: Int = 1
    @KtSetter set(value) {}

@KtPropertyGetter
val ktPropertyGetterTarget: Int = 1

@KtPropertySetter
var ktPropertySetterTarget: Int = 1

@KtGetterSetter
var ktGetterSetterTarget: Int = 1

@KtPropertyGetterSetter
var ktPropertyGetterSetterTarget: Int = 1

class KtConstructorPropertyTargets(
    @KtValueParam @JavaParam val vp: Int,
    @KtProperty @JavaMethod val p: Int,
    @KtField @JavaField val f: Int,
    @KtValueParamProperty @JavaParamMethod val vpp: Int,
    @KtValueParamField @JavaParamField val vpf: Int,
    @KtPropertyField @JavaFieldMethod val pf: Int,
    @KtValueParamPropertyField @JavaParamFieldMethod val vppf: Int
)

// FILE: JavaMain.java
@KtEmptyTargets @JavaEmptyTargets
class JavaEmptyTargetClass {}

@KtDefaultTargets @JavaDefaultTargets
class JavaDefaultTargetClass {}

@KtClass @JavaType
class JavaMain<@KtTypeParam @JavaTypeParam T> {
    @KtField @JavaField
    int javaFieldTarget;

    @KtConstructor @JavaConstructor
    JavaMain() {}

    @KtFunction @JavaMethod
    void javaMethodTarget(@KtValueParam @JavaParam int p) {
        @KtLocalVar @JavaLocalVar int l = 42;
    }
}

@KtAnnoClass @JavaAnnoType
@interface JavaAnnoTarget {}

@KtClassAnnoClass @JavaTypeAnnoType
@interface JavaClassAnnoTarget {}
