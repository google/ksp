/*
 * Copyright 2024 Google LLC
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// MyEnum : Enum<MyEnum>
// <init> : MyEnum
// values : Array<MyEnum>
// value : String
// valueOf : MyEnum
// entries : EnumEntries<MyEnum>
// entries.getter() : EnumEntries<MyEnum>
// VarianceSubjectSuppressed : Any
// R : Any?
// propWithFinalType : String
// propWithFinalType.getter() : String
// value : String
// propWithOpenType : Number
// propWithOpenType.getter() : Number
// value : Number
// propWithFinalGeneric : List<String>
// propWithFinalGeneric.getter() : List<String>
// value : List<String>
// propWithOpenGeneric : List<Number>
// propWithOpenGeneric.getter() : List<Number>
// value : List<Number>
// propWithTypeArg : R
// propWithTypeArg.getter() : R
// value : R
// propWithTypeArgGeneric : List<R>
// propWithTypeArgGeneric.getter() : List<R>
// value : List<R>
// propWithOpenTypeButSuppressAnnotation : Number
// propWithOpenTypeButSuppressAnnotation.getter() : Number
// value : Number
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
// list7 : List<out String>
// explicitJvmWildcard : List<out String>
// list8 : List<Number>
// explicitJvmSuppressWildcard_OnType : List<Number>
// list9 : List<Number>
// explicitJvmSuppressWildcard_OnType2 : List<Number>
// starList : List<Any?>
// typeArgList : List<R>
// numberList : List<Number>
// stringList : List<String>
// enumList : List<MyEnum>
// jvmWildcard : List<out String>
// suppressJvmWildcard : List<Number>
// <init> : VarianceSubjectSuppressed<out Any?>
// R : Any?
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
