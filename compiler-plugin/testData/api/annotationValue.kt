/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o.
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
// Str
// 42
// Foo
// File
// Local
// @Foo
// @Suppress
// G
// 31
// END

enum class RGB {
    R, G, B
}

annotation class Foo(val s: Int)

annotation class Bar(
    val argStr: String,
    val argInt: Int,
    val argClsUser: kotlin.reflect.KClass<*>,
    val argClsLib: kotlin.reflect.KClass<*>,
    val argClsLocal: kotlin.reflect.KClass<*>,
    val argAnnoUser: Foo,
    val argAnnoLib: Suppress,
    val argEnum: RGB,
    val argDef: Int = 31
)

fun Fun() {
    @Bar("Str", 42, Foo::class, java.io.File::class, Local::class, Foo(17), Suppress("name1", "name2"), RGB.G)
    class Local
}
