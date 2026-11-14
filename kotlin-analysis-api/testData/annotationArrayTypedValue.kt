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
// PROCESSOR INPUT: JavaAnnotation, KotlinAnnotation, TypedAnnotation, JavaPrimitiveAnnotation, KotlinPrimitiveAnnotation, UnsignedAnnotation, NullableAnnotation, KotlinArrayValues
// EXPECTED:
// EmptyArrayAnnotated @KotlinArrayValues classes = ArrayValue[]
// EmptyArrayAnnotated @KotlinArrayValues entries = ArrayValue[]
// EmptyArrayAnnotated @KotlinArrayValues nested = AnnotationClass(KotlinNestedAnnotation(value=Primitive(KSString("empty"))))
// EmptyArrayAnnotated @KotlinArrayValues numbers = ArrayValue[]
// EmptyArrayAnnotated @KotlinArrayValues strings = ArrayValue[]
// JavaAnnotated @JavaAnnotation args = ArrayValue[AnnotationClass(NestedAnnotation(value=Primitive(KSString("one")))), AnnotationClass(NestedAnnotation(value=Primitive(KSString("two"))))]
// JavaAnnotated @KotlinAnnotation args = ArrayValue[AnnotationClass(KotlinNestedAnnotation(value=Primitive(KSString("one")))), AnnotationClass(KotlinNestedAnnotation(value=Primitive(KSString("two"))))]
// JavaAnnotated @TypedAnnotation klass = ReflectionClassReference(String)
// JavaAnnotated @TypedAnnotation mode = EnumEntry(TestMode.FIRST)
// JavaAnnotated @TypedAnnotation number = Primitive(KSInt(9))
// JavaArrayAnnotated @KotlinArrayValues classes = ArrayValue[ReflectionClassReference(String)]
// JavaArrayAnnotated @KotlinArrayValues entries = ArrayValue[EnumEntry(TestMode.FIRST)]
// JavaArrayAnnotated @KotlinArrayValues nested = AnnotationClass(KotlinNestedAnnotation(value=Primitive(KSString("java-nested"))))
// JavaArrayAnnotated @KotlinArrayValues numbers = ArrayValue[Primitive(KSInt(3)), Primitive(KSInt(4))]
// JavaArrayAnnotated @KotlinArrayValues strings = ArrayValue[Primitive(KSString("java-left")), null]
// JavaNullAnnotated @NullableAnnotation value = null
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation boolValue = Primitive(KSBool(true))
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation byteValue = Primitive(KSInt(-7))
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation charValue = Primitive(KSChar('x'))
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation doubleValue = Primitive(KSDouble(2.5))
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation floatValue = Primitive(KSFloat(1.5))
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation intValue = Primitive(KSInt(-42))
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation longValue = Primitive(KSLong(9876543210))
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation shortValue = Primitive(KSInt(12))
// JavaPrimitiveAnnotated @JavaPrimitiveAnnotation stringValue = Primitive(KSString("java"))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation boolValue = Primitive(KSBool(true))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation byteValue = Primitive(KSInt(-7))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation charValue = Primitive(KSChar('x'))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation doubleValue = Primitive(KSDouble(2.5))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation floatValue = Primitive(KSFloat(1.5))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation intValue = Primitive(KSInt(-42))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation longValue = Primitive(KSLong(9876543210))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation shortValue = Primitive(KSInt(12))
// JavaPrimitiveAnnotated @KotlinPrimitiveAnnotation stringValue = Primitive(KSString("java"))
// KotlinAnnotated @JavaAnnotation args = ArrayValue[AnnotationClass(NestedAnnotation(value=Primitive(KSString("one")))), AnnotationClass(NestedAnnotation(value=Primitive(KSString("two"))))]
// KotlinAnnotated @KotlinAnnotation args = ArrayValue[AnnotationClass(KotlinNestedAnnotation(value=Primitive(KSString("one")))), AnnotationClass(KotlinNestedAnnotation(value=Primitive(KSString("two"))))]
// KotlinAnnotated @TypedAnnotation klass = ReflectionClassReference(String)
// KotlinAnnotated @TypedAnnotation mode = EnumEntry(TestMode.FIRST)
// KotlinAnnotated @TypedAnnotation number = Primitive(KSInt(7))
// KotlinArrayAnnotated @KotlinArrayValues classes = ArrayValue[ReflectionClassReference(String), ReflectionClassReference(Int)]
// KotlinArrayAnnotated @KotlinArrayValues entries = ArrayValue[EnumEntry(TestMode.FIRST)]
// KotlinArrayAnnotated @KotlinArrayValues nested = AnnotationClass(KotlinNestedAnnotation(value=Primitive(KSString("nested"))))
// KotlinArrayAnnotated @KotlinArrayValues numbers = ArrayValue[Primitive(KSInt(1)), Primitive(KSInt(2))]
// KotlinArrayAnnotated @KotlinArrayValues strings = ArrayValue[Primitive(KSString("left")), null, Primitive(KSString("right"))]
// KotlinNullAnnotated @NullableAnnotation value = null
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation boolValue = Primitive(KSBool(false))
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation byteValue = Primitive(KSByte(7))
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation charValue = Primitive(KSChar('y'))
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation doubleValue = Primitive(KSDouble(-2.5))
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation floatValue = Primitive(KSFloat(-1.5))
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation intValue = Primitive(KSInt(42))
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation longValue = Primitive(KSLong(-9876543210))
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation shortValue = Primitive(KSShort(-12))
// KotlinPrimitiveAnnotated @JavaPrimitiveAnnotation stringValue = Primitive(KSString("kotlin"))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation boolValue = Primitive(KSBool(false))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation byteValue = Primitive(KSByte(7))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation charValue = Primitive(KSChar('y'))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation doubleValue = Primitive(KSDouble(-2.5))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation floatValue = Primitive(KSFloat(-1.5))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation intValue = Primitive(KSInt(42))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation longValue = Primitive(KSLong(-9876543210))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation shortValue = Primitive(KSShort(-12))
// KotlinPrimitiveAnnotated @KotlinPrimitiveAnnotation stringValue = Primitive(KSString("kotlin"))
// UnsignedAnnotated @UnsignedAnnotation byteValue = Primitive(KSUByte(255))
// UnsignedAnnotated @UnsignedAnnotation intValue = Primitive(KSUInt(4294967295))
// UnsignedAnnotated @UnsignedAnnotation longValue = Primitive(KSULong(18446744073709551615))
// UnsignedAnnotated @UnsignedAnnotation shortValue = Primitive(KSUShort(65535))
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
