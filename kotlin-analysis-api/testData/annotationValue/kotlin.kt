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
// TEST PROCESSOR: AnnotationArgumentProcessor
// EXPECTED:
// defaultInNested
// SomeClass$WithDollarSign
// Str
// 42
// Foo
// File
// Local
// Array
// Error type synthetic declaration
// [<ERROR TYPE>, Foo]
// @Foo
// @Suppress
// RGB.G
// JavaEnum.ONE
// 31
// Throws
// END
// FILE: a.kt

enum class RGB {
    R, G, B
}

class ThrowsClass {
    @Throws(Exception::class)
    protected open fun throwsException() {
    }
}

annotation class Foo(val s: Int) {
    annotation class Nested(val nestedDefault:String = "defaultInNested")
}
class `SomeClass$WithDollarSign`

annotation class MyAnnotation(val clazz: KClass<*>)


annotation class Bar(
    val argStr: String,
    val argInt: Int,
    val argClsUser: kotlin.reflect.KClass<*>,
    val argClsLib: kotlin.reflect.KClass<*>,
    val argClsLocal: kotlin.reflect.KClass<*>,
    val argClsArray: kotlin.reflect.KClass<*>,
    val argClsMissing: kotlin.reflect.KClass<*>,
    val argClsMissingInArray: Array<kotlin.reflect.KClass<*>>,
    val argAnnoUser: Foo,
    val argAnnoLib: Suppress,
    val argEnum: RGB,
    val argJavaNum: JavaEnum,
    val argDef: Int = 31
)

fun Fun() {
    @Foo.Nested
    @MyAnnotation(`SomeClass$WithDollarSign`::class)
    @Bar(
        "Str",
        40 + 2,
        Foo::class,
        java.io.File::class,
        Local::class,
        Array<String>::class,
        Missing::class,
        [Missing::class, Foo::class],
        Foo(17),
        Suppress("name1", "name2"),
        RGB.G,
        JavaEnum.ONE
    )
    class Local
}

// FILE: JavaEnum.java

enum JavaEnum { ONE, TWO, THREE }
