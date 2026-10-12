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

// TEST PROCESSOR: MultiplatformAnnotationArgumentsProcessor
// PROCESSOR INPUT: lib.MyAnnotation
// EXPECTED:
// @MyAnnotation Foo default arguments: withDefaultValue = the default value, intValue = 42, enumValue = MyEnum.A, classValue = Nested, nestedValue = @Nested, intArrayValue = [1, 2, 3], stringArrayValue = [a, b]
// @MyAnnotation Foo arguments: noDefaultValue = foo, noDefaultArrayValue = [foo array], withDefaultValue = the default value, intValue = 42, enumValue = MyEnum.A, classValue = Nested, nestedValue = @Nested, intArrayValue = [1, 2, 3], stringArrayValue = [a, b]
// @MyAnnotation Bar default arguments: withDefaultValue = the default value, intValue = 42, enumValue = MyEnum.A, classValue = Nested, nestedValue = @Nested, intArrayValue = [1, 2, 3], stringArrayValue = [a, b]
// @MyAnnotation Bar arguments: noDefaultValue = bar, noDefaultArrayValue = [bar array], withDefaultValue = some other value, intValue = 7, enumValue = MyEnum.B, classValue = Foo, nestedValue = @Nested, intArrayValue = [4, 5], stringArrayValue = [c]
// @MyAnnotation LibFoo default arguments: withDefaultValue = the default value, intValue = 42, enumValue = MyEnum.A, classValue = Nested, nestedValue = @Nested, intArrayValue = [1, 2, 3], stringArrayValue = [a, b]
// @MyAnnotation LibFoo arguments: noDefaultValue = libFoo, noDefaultArrayValue = [libFoo array], withDefaultValue = the default value, intValue = 42, enumValue = MyEnum.A, classValue = Nested, nestedValue = @Nested, intArrayValue = [1, 2, 3], stringArrayValue = [a, b]
// END

// NOTE: This test is a duplicate of the native annotationDefaultValueTypes to assert that both JVM and native return the same results.

// MODULE: lib
// FILE: Lib.kt
package lib

import kotlin.reflect.KClass

enum class MyEnum {
    A,
    B
}

annotation class Nested(val value: String = "the default nested value")

// `noDefaultValue` and `noDefaultArrayValue` have no default value, so they must not show up in
// `KSAnnotation.defaultArguments`. In particular, `noDefaultArrayValue` must not show up as an empty array.
// The remaining parameters cover default value conversions on JVM.
annotation class MyAnnotation(
    val noDefaultValue: String,
    val noDefaultArrayValue: Array<String>,
    val withDefaultValue: String = "the default value",
    val intValue: Int = 42,
    val enumValue: MyEnum = MyEnum.A,
    val classValue: KClass<*> = Nested::class,
    val nestedValue: Nested = Nested(),
    val intArrayValue: IntArray = [1, 2, 3],
    val stringArrayValue: Array<String> = ["a", "b"]
)

// Annotated inside the library, so that the annotation is backed by `KSAnnotationResolvedImpl`.
@MyAnnotation(noDefaultValue = "libFoo", noDefaultArrayValue = ["libFoo array"])
class LibFoo

// MODULE: main(lib)
// FILE: Main.kt
import lib.MyAnnotation
import lib.MyEnum
import lib.Nested

// Omit all arguments that have default values, supplying only required ones.
@MyAnnotation(
    noDefaultValue = "foo",
    noDefaultArrayValue = ["foo array"],
)
class Foo

// Explicitly supply every argument, overriding every default.
@MyAnnotation(
    noDefaultValue = "bar",
    noDefaultArrayValue = ["bar array"],
    withDefaultValue = "some other value",
    intValue = 7,
    enumValue = MyEnum.B,
    classValue = Foo::class,
    nestedValue = Nested("explicit nested value"),
    intArrayValue = [4, 5],
    stringArrayValue = ["c"],
)
class Bar
