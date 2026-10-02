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
// This is a copy of annotationArrayValueType.kt with a different expected output.
// Keep the source fixtures in both files in sync.
// TEST PROCESSOR: AnnotationArrayTypedValueProcessor
// EXPECTED:
// JavaAnnotated JavaAnnotation args typedValue: ArrayValue of two AnnotationClass values
// JavaAnnotated KotlinAnnotation args typedValue: ArrayValue of two AnnotationClass values
// KotlinAnnotated JavaAnnotation args typedValue: ArrayValue of two AnnotationClass values
// KotlinAnnotated KotlinAnnotation args typedValue: ArrayValue of two AnnotationClass values
// KotlinAnnotated TypedAnnotation number: Primitive(KSInt(7))
// KotlinAnnotated TypedAnnotation mode: EnumClass(FIRST)
// KotlinAnnotated TypedAnnotation klass: ReflectionClassReference(String)
// END
// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    NestedAnnotation[] args();
}

// FILE: NestedAnnotation.java
public @interface NestedAnnotation {
    String value();
}

// FILE: JavaAnnotated.java
@JavaAnnotation(args = {@NestedAnnotation(value = "one"), @NestedAnnotation(value = "two")})
@KotlinAnnotation(args = {@KotlinNestedAnnotation(value = "one"), @KotlinNestedAnnotation(value = "two")})
class JavaAnnotated {}

// FILE: KotlinAnnotated.kt
annotation class KotlinAnnotation(val args: Array<KotlinNestedAnnotation>)

annotation class KotlinNestedAnnotation(val value: String)

enum class TestMode { FIRST }

annotation class TypedAnnotation(
    val number: Int,
    val mode: TestMode,
    val klass: kotlin.reflect.KClass<*>,
)

@JavaAnnotation(args = [NestedAnnotation(value = "one"), NestedAnnotation(value = "two")])
@KotlinAnnotation(args = [KotlinNestedAnnotation(value = "one"), KotlinNestedAnnotation(value = "two")])
@TypedAnnotation(number = 7, mode = TestMode.FIRST, klass = String::class)
class KotlinAnnotated
