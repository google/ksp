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
// TEST PROCESSOR: AnnotationArrayValueTypeProcessor
// EXPECTED:
// JavaAnnotated JavaAnnotation args is Array<*> or Collection<*>: true
// JavaAnnotated JavaAnnotation args size: 2
// JavaAnnotated KotlinAnnotation args is Array<*> or Collection<*>: true
// JavaAnnotated KotlinAnnotation args size: 2
// KotlinAnnotated JavaAnnotation args is Array<*> or Collection<*>: true
// KotlinAnnotated JavaAnnotation args size: 2
// KotlinAnnotated KotlinAnnotation args is Array<*> or Collection<*>: true
// KotlinAnnotated KotlinAnnotation args size: 2
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
@TypedAnnotation(number = 9, mode = TestMode.FIRST, klass = String.class)
class JavaAnnotated {}

// FILE: JavaPrimitiveAnnotation.java
public @interface JavaPrimitiveAnnotation {
    boolean boolValue();
    byte byteValue();
    short shortValue();
    int intValue();
    long longValue();
    float floatValue();
    double doubleValue();
    char charValue();
    String stringValue();
}

// FILE: JavaPrimitiveAnnotated.java
// KSP evaluates these Java byte/short literals as Int. typedValue() wraps the actual value,
// rather than coercing it to the annotation parameter's declared type.
@JavaPrimitiveAnnotation(
    boolValue = true, byteValue = -7, shortValue = 12, intValue = -42,
    longValue = 9876543210L, floatValue = 1.5f, doubleValue = 2.5,
    charValue = 'x', stringValue = "java"
)
@KotlinPrimitiveAnnotation(
    boolValue = true, byteValue = -7, shortValue = 12, intValue = -42,
    longValue = 9876543210L, floatValue = 1.5f, doubleValue = 2.5,
    charValue = 'x', stringValue = "java"
)
class JavaPrimitiveAnnotated {}

// FILE: JavaNullAnnotated.java
// Null annotation arguments are intentional: KSP also processes erroneous source.
@NullableAnnotation(value = null)
class JavaNullAnnotated {}

// FILE: JavaArrayAnnotated.java
@KotlinArrayValues(
    numbers = {3, 4}, entries = {TestMode.FIRST}, classes = {String.class},
    nested = @KotlinNestedAnnotation(value = "java-nested"), strings = {"java-left", null}
)
class JavaArrayAnnotated {}

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

annotation class KotlinPrimitiveAnnotation(
    val boolValue: Boolean,
    val byteValue: Byte,
    val shortValue: Short,
    val intValue: Int,
    val longValue: Long,
    val floatValue: Float,
    val doubleValue: Double,
    val charValue: Char,
    val stringValue: String,
)

@JavaPrimitiveAnnotation(
    boolValue = false, byteValue = 7, shortValue = -12, intValue = 42,
    longValue = -9876543210L, floatValue = -1.5f, doubleValue = -2.5,
    charValue = 'y', stringValue = "kotlin",
)
@KotlinPrimitiveAnnotation(
    boolValue = false, byteValue = 7, shortValue = -12, intValue = 42,
    longValue = -9876543210L, floatValue = -1.5f, doubleValue = -2.5,
    charValue = 'y', stringValue = "kotlin",
)
class KotlinPrimitiveAnnotated

annotation class UnsignedAnnotation(
    val byteValue: UByte,
    val shortValue: UShort,
    val intValue: UInt,
    val longValue: ULong,
)

@UnsignedAnnotation(255u, 65535u, 4294967295u, 18446744073709551615uL)
class UnsignedAnnotated

// Null annotation arguments are intentional: KSP also processes erroneous source.
annotation class NullableAnnotation(val value: String?)

@NullableAnnotation(null)
class KotlinNullAnnotated

annotation class KotlinArrayValues(
    val numbers: IntArray,
    val entries: Array<TestMode>,
    val classes: Array<kotlin.reflect.KClass<*>>,
    val nested: KotlinNestedAnnotation,
    val strings: Array<String?>,
)

@KotlinArrayValues(
    numbers = [1, 2], entries = [TestMode.FIRST], classes = [String::class, Int::class],
    nested = KotlinNestedAnnotation("nested"), strings = ["left", null, "right"],
)
class KotlinArrayAnnotated

@KotlinArrayValues(
    numbers = [], entries = [], classes = [],
    nested = KotlinNestedAnnotation("empty"), strings = [],
)
class EmptyArrayAnnotated
