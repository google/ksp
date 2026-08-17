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

// TEST PROCESSOR: TypeAnnotationClassReferenceProcessor
// EXPECTED:
// LibFoo bar default value: String
// LibFoo baz value: String
// LibFoo listBar default value: String
// LibFoo listBaz value: String
// LibFoo nestedDefault value: Int
// LibFoo nestedExplicit value: String
// LibJavaFoo bar default value: String
// LibJavaFoo baz value: String
// LibJavaFoo listBar default value: String
// LibJavaFoo listBaz value: String
// LibJavaFoo nestedExplicit value: String
// MainFoo bar default value: String
// MainFoo baz value: String
// MainFoo listBar default value: String
// MainFoo listBaz value: String
// MainFoo nestedDefault value: Int
// MainFoo nestedExplicit value: String
// END
// MODULE: lib
// FILE: lib.kt
import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MyAnno(val value: KClass<*> = String::class)

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class MyAnnoNoDefault(val value: KClass<*>)

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class OuterAnno(val inner: MyAnno = MyAnno(Int::class))

@Target(AnnotationTarget.TYPE, AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class OuterAnnoNoDefault(val inner: MyAnnoNoDefault)

class LibFoo {
    val bar: @MyAnno Int = 0
    val baz: @MyAnnoNoDefault(String::class) Int = 0
    val listBar: List<@MyAnno Int> = emptyList()
    val listBaz: List<@MyAnnoNoDefault(String::class) Int> = emptyList()
    @OuterAnno
    val nestedDefault: Int = 0
    @OuterAnnoNoDefault(MyAnnoNoDefault(String::class))
    val nestedExplicit: Int = 0
}

// FILE: JavaAnno.java
import java.lang.annotation.*;

@Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
public @interface JavaAnno {
    Class<?> value() default String.class;
}

// FILE: JavaAnnoNoDefault.java
import java.lang.annotation.*;

@Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
public @interface JavaAnnoNoDefault {
    Class<?> value();
}

// FILE: JavaOuterAnno.java
import java.lang.annotation.*;

@Target({ElementType.TYPE_USE, ElementType.TYPE, ElementType.FIELD})
public @interface JavaOuterAnno {
    JavaAnnoNoDefault inner();
}

// FILE: LibJavaFoo.java
import java.util.List;

public class LibJavaFoo {
    public @JavaAnno int bar = 0;
    public @JavaAnnoNoDefault(String.class) int baz = 0;
    public List<@JavaAnno Integer> listBar = null;
    public List<@JavaAnnoNoDefault(String.class) Integer> listBaz = null;
    @JavaOuterAnno(inner = @JavaAnnoNoDefault(String.class))
    public int nestedExplicit = 0;
}

// MODULE: main(lib)
// FILE: main.kt
class MainFoo {
    val bar: @MyAnno Int = 0
    val baz: @MyAnnoNoDefault(String::class) Int = 0
    val listBar: List<@MyAnno Int> = emptyList()
    val listBaz: List<@MyAnnoNoDefault(String::class) Int> = emptyList()
    @OuterAnno
    val nestedDefault: Int = 0
    @OuterAnnoNoDefault(MyAnnoNoDefault(String::class))
    val nestedExplicit: Int = 0
}
