import kotlin.reflect.KClass

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

// TEST PROCESSOR: AnnotationDefaultValueProcessor
// EXPECTED:
// KotlinAnnotation -> a:debugKt,b:default
// JavaAnnotation -> debug:debug,withDefaultValue:OK
// JavaAnnotation2 -> y:y-kotlin,x:x-kotlin,z:z-default
// KotlinAnnotation2 -> y:y-kotlin,x:x-kotlin,z:z-default
// KotlinAnnotation -> a:debugJava,b:default
// JavaAnnotation -> debug:debugJava2,withDefaultValue:OK
// JavaAnnotation2 -> y:y-java,x:x-java,z:z-default
// KotlinAnnotation2 -> y:y-java,x:x-java,z:z-default
// END
// FILE: a.kt

annotation class Test

@Suppress("LongParameterList")
annotation class ParametersTestAnnotation(
    val booleanValue: Boolean = false,
    val byteValue: Byte = 2,
    val charValue: Char = 'b',
    val doubleValue: Double = 3.0,
    val floatValue: Float = 4.0f,
    val intValue: Int = 5,
    val longValue: Long = 6L,
    val stringValue: String = "emptystring",
    // fails on getting the arguments from the KSAnnotation when no value is set for the kClassValue in
    // the declaration. Throws an NPE with using a default value
    val kClassValue: KClass<*> = ParametersTestAnnotation::class,
    val enumValue: TestEnum = TestEnum.NONE,
)

@Suppress("LongParameterList")
annotation class ParameterArraysTestAnnotation(
    val booleanArrayValue: BooleanArray = booleanArrayOf(),
    val byteArrayValue: ByteArray = byteArrayOf(),
    val charArrayValue: CharArray = charArrayOf(),
    val doubleArrayValue: DoubleArray = doubleArrayOf(),
    val floatArrayValue: FloatArray = floatArrayOf(),
    val intArrayValue: IntArray = intArrayOf(),
    val longArrayValue: LongArray = longArrayOf(),
    val stringArrayValue: Array<String> = emptyArray(),
    val kClassArrayValue: Array<KClass<*>> = emptyArray(),
    val enumArrayValue: Array<TestEnum> = emptyArray(),
)

annotation class ParametersTestWithNegativeDefaultsAnnotation(
    val byteValue: Byte = -2,
    val doubleValue: Double = -3.0,
    val floatValue: Float = -4.0f,
    val intValue: Int = -5,
    val longValue: Long = -6L,
)

enum class TestEnum {
    NONE, VALUE1, VALUE2
}

@ParametersTestAnnotation(
    booleanValue = true,
    byteValue = 5,
    charValue = 'k',
    doubleValue = 5.12,
    floatValue = 123.3f,
    intValue = 2,
    longValue = 4L,
    stringValue = "someValue",
    Throwable::class,
    TestEnum.VALUE1
)
@Test
class A

// FILE: JavaTestAnnotations.java
class JavaTestAnnotations {
    annotation class JavaParametersTestAnnotation(
        val booleanValue: Boolean = true,
        val byteValue: Byte = -2,
        val charValue: Char = 'b',
        val doubleValue: Double = -3.0,
        val floatValue: Float = -4.0f,
        val intValue: Int = -5,
        val longValue: Long = -6L,
        val stringValue: String = "emptystring",
        val classValue: KClass<*>,
        val enumValue: JavaTestEnum = JavaTestEnum.NONE
    )

    annotation class JavaParameterArraysTestAnnotation(
        val booleanArrayValue: BooleanArray = [true, false],
        val byteArrayValue: ByteArray = [],
        val charArrayValue: CharArray = [],
        val doubleArrayValue: DoubleArray = [],
        val floatArrayValue: FloatArray = [],
        val intArrayValue: IntArray = [],
        val longArrayValue: LongArray = [],
        val stringArrayValue: Array<String> = [],
        val classArrayValue: Array<KClass<*>> = [],
        val enumArrayValue: Array<JavaTestEnum> = []
    )

    enum class JavaTestEnum {
        NONE, VALUE1, VALUE2
    }
}
