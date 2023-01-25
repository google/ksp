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
// TEST PROCESSOR: JavaWildcard2Processor
// EXPECTED:
// MyEnum : Any
// <init> : MyEnum
// VarianceSubjectSuppressed : Any
// R : Any?
// starList : List<Any?>
// typeArgList : List<R>
// numberList : List<Number>
// stringList : List<String>
// enumList : List<MyEnum>
// jvmWildcard : List<out [@kotlin.jvm.JvmWildcard] String>
// suppressJvmWildcard : List<[@kotlin.jvm.JvmSuppressWildcards] Number>
// <init> : VarianceSubjectSuppressed<R>
// propWithFinalType : String
// propWithFinalType.getter() : String
// <set-?> : String
// propWithOpenType : Number
// propWithOpenType.getter() : Number
// <set-?> : Number
// propWithFinalGeneric : List<String>
// propWithFinalGeneric.getter() : List<String>
// <set-?> : List<String>
// propWithOpenGeneric : List<Number>
// propWithOpenGeneric.getter() : List<Number>
// <set-?> : List<Number>
// propWithTypeArg : R
// propWithTypeArg.getter() : R
// <set-?> : R
// propWithTypeArgGeneric : List<R>
// propWithTypeArgGeneric.getter() : List<R>
// <set-?> : List<R>
// propWithOpenTypeButSuppressAnnotation : Number
// propWithOpenTypeButSuppressAnnotation.getter() : Number
// <set-?> : Number
// list2 : List<Any?>
// list1 : List<Any?>
// list3 : List<R>
// listTypeArg : List<R>
// list4 : List<Number>
// listTypeArgNumber : List<Number>
// list5 : List<String>
// listTypeArgString : List<String>
// list6 : List<MyEnum>
// listTypeArgEnum : List<MyEnum>
// list7 : List<out [@kotlin.jvm.JvmWildcard] String>
// explicitJvmWildcard : List<out [@kotlin.jvm.JvmWildcard] String>
// list8 : List<[@kotlin.jvm.JvmSuppressWildcards] Number>
// explicitJvmSuppressWildcard_OnType : List<[@kotlin.jvm.JvmSuppressWildcards] Number>
// list9 : [@kotlin.jvm.JvmSuppressWildcards] List<Number>
// explicitJvmSuppressWildcard_OnType2 : [@kotlin.jvm.JvmSuppressWildcards] List<Number>
// END

enum class MyEnum()

@JvmSuppressWildcards
class VarianceSubjectSuppressed<R>(
    starList: List<*>,
    typeArgList: List<R>,
    numberList: List<Number>,
    stringList: List<String>,
    enumList: List<MyEnum>,
    jvmWildcard: List<@JvmWildcard String>,
    suppressJvmWildcard: List<@JvmSuppressWildcards Number>
) {
    var propWithFinalType: String = ""
    var propWithOpenType: Number = 3
    var propWithFinalGeneric: List<String> = TODO()
    var propWithOpenGeneric: List<Number> = TODO()
    var propWithTypeArg: R = TODO()
    var propWithTypeArgGeneric: List<R> = TODO()
    @JvmSuppressWildcards
    var propWithOpenTypeButSuppressAnnotation: Number = 3
    fun list1(list2: List<*>): List<*> { TODO() }
    fun listTypeArg(list3: List<R>): List<R> { TODO() }
    fun listTypeArgNumber(list4: List<Number>): List<Number> { TODO() }
    fun listTypeArgString(list5: List<String>): List<String> { TODO() }
    fun listTypeArgEnum(list6: List<MyEnum>): List<MyEnum> { TODO() }
    fun explicitJvmWildcard(
        list7: List<@JvmWildcard String>
    ): List<@JvmWildcard String> { TODO() }

    fun explicitJvmSuppressWildcard_OnType(
        list8: List<@JvmSuppressWildcards Number>
    ): List<@JvmSuppressWildcards Number> { TODO() }

    fun explicitJvmSuppressWildcard_OnType2(
        list9: @JvmSuppressWildcards List<Number>
    ): @JvmSuppressWildcards List<Number> { TODO() }
}
