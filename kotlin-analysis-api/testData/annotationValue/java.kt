/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// Error type synthetic declaration
// Array
// @Foo
// @Suppress
// RGB
// JavaEnum
// 31
// [warning1, warning 2]
// END
// MODULE: module1
// FILE: placeholder.kt
// FILE: TestLib.java
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
@Target({ElementType.TYPE, ElementType.TYPE_USE})
@interface MyAnnotation {
    String stringParam() default "1";
    String stringParam2() default "1";
    String[] stringArrayParam() default {"3", "5", "7"};
}
interface MyInterface {}
@MyAnnotation(stringParam = "2") class MyClassInLib implements MyInterface {}
// MODULE: main(module1)
// FILE: Test.java
@MyAnnotation(stringParam = "2") class MyClass implements MyInterface {}

// FILE: a.kt
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
    val argClsArray: kotlin.reflect.KClass<*>,
    val argAnnoUser: Foo,
    val argAnnoLib: Suppress,
    val argEnum: RGB,
    val argJavaNum: JavaEnum,
    val argDef: Int = 31
)

// FILE: C.java

@SuppressWarnings({"warning1", "warning 2"})
class C {

}
// FILE: JavaAnnotated.java
@Bar(argStr = "Str",
    argInt = 40 + 2,
    argClsUser = Foo.class,
        argClsLib = java.io.File.class,
argClsLocal = Local.class, // intentional error type
argClsArray = kotlin.Array.class,
argAnnoUser = @Foo(s = 17),
argAnnoLib = @Suppress(names = {"name1", "name2"}),
argEnum = RGB.G,
argJavaNum = JavaEnum.ONE)
public class JavaAnnotated {}

// FILE: JavaEnum.java

enum JavaEnum { ONE, TWO, THREE }
