/*
 * Copyright 2021 Google LLC
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: AnnotatedUtilProcessor
// EXPECTED:
// Test: OnlyTestAnnotation
// Test: ParametersTestAnnotationWithValuesTest
// IsPresent: class com.google.devtools.ksp.processor.ParametersTestAnnotation
// ByType: com.google.devtools.ksp.processor.ParametersTestAnnotation[booleanValue=true, byteValue=5, shortValue=202, charValue=k, doubleValue=5.12, floatValue=123.3, intValue=2, longValue=4, stringValue=someValue, kClassValue=class java.lang.Throwable, enumValue=VALUE1]
// Test: ParametersTestAnnotationWithIntegerLiteralValuesTest
// IsPresent: class com.google.devtools.ksp.processor.ParametersTestAnnotation
// ByType: com.google.devtools.ksp.processor.ParametersTestAnnotation[booleanValue=true, byteValue=5, shortValue=202, charValue=k, doubleValue=5.0, floatValue=123.0, intValue=2, longValue=4, stringValue=someValue, kClassValue=class java.lang.Throwable, enumValue=VALUE1]
// Test: ParametersTestAnnotationWithDefaultsTest
// IsPresent: class com.google.devtools.ksp.processor.ParametersTestAnnotation
// ByType: com.google.devtools.ksp.processor.ParametersTestAnnotation[booleanValue=false, byteValue=2, shortValue=3, charValue=b, doubleValue=4.0, floatValue=5.0, intValue=6, longValue=7, stringValue=emptystring, kClassValue=interface com.google.devtools.ksp.processor.ParametersTestAnnotation, enumValue=NONE]
// ByType: com.google.devtools.ksp.processor.ParametersTestAnnotation[booleanValue=false, byteValue=2, shortValue=3, charValue=b, doubleValue=4.0, floatValue=5.0, intValue=6, longValue=7, stringValue=emptystring, kClassValue=interface com.google.devtools.ksp.processor.ParametersTestAnnotation, enumValue=NONE]
// Test: ParametersTestWithNegativeDefaultsAnnotationTest
// IsPresent: class com.google.devtools.ksp.processor.ParametersTestWithNegativeDefaultsAnnotation
// ByType: com.google.devtools.ksp.processor.ParametersTestWithNegativeDefaultsAnnotation[byteValue=-2, shortValue=-3, doubleValue=-4.0, floatValue=-5.0, intValue=-6, longValue=-7]
// ByType: com.google.devtools.ksp.processor.ParametersTestWithNegativeDefaultsAnnotation[byteValue=-2, shortValue=-3, doubleValue=-4.0, floatValue=-5.0, intValue=-6, longValue=-7]
// Test: ParameterArraysTestAnnotationWithDefaultTest
// IsPresent: class com.google.devtools.ksp.processor.ParameterArraysTestAnnotation
// ByType: ParameterArraysTestAnnotation[booleanArrayValue=[true, false],byteArrayValue=[-2, 4],shortArrayValue=[-1, 2, 3],charArrayValue=[a, b, c],doubleArrayValue=[1.1, 2.2, 3.3],floatArrayValue=[1.0, 2.0, 3.3],intArrayValue=[1, 2, 4, 8, 16],longArrayValue=[1, 2, 4, 8, 16, 32],stringArrayValue=[first, second, third],kClassArrayValue=[class kotlin.Throwable, class com.google.devtools.ksp.processor.ParametersTestAnnotation],enumArrayValue=[VALUE1, VALUE2, VALUE1, VALUE2]]
// Test: ParameterArraysTestAnnotationWithSingleAsArrayTest
// IsPresent: class com.google.devtools.ksp.processor.ParameterArraysTestAnnotation
// ByType: ParameterArraysTestAnnotation[booleanArrayValue=[true],byteArrayValue=[-2],shortArrayValue=[-1],charArrayValue=[a],doubleArrayValue=[1.1],floatArrayValue=[1.0],intArrayValue=[1],longArrayValue=[1],stringArrayValue=[first],kClassArrayValue=[class kotlin.Throwable],enumArrayValue=[VALUE1]]
// Test: AnnotationWithinAnAnnotationTest
// IsPresent: class com.google.devtools.ksp.processor.OuterAnnotation
// ByType: com.google.devtools.ksp.processor.OuterAnnotation[innerAnnotation=com.google.devtools.ksp.processor.InnerAnnotation[value=hello from the other side]]
// END
// FILE: com/google/devtools/ksp/processor/A.java
package com.google.devtools.ksp.processor;

import kotlin.reflect.KClass

public class A {}


public @interface Test {}

public @interface ParametersTestAnnotation {
    Boolean booleanValue() default false;
    Byte byteValue() default 2;
    Short shortValue() default 3;
    Char charValue() default 'b';
    Double doubleValue() default 4.0;
    Float floatValue() default 5.0f;
    Int intValue() default 6;
    Long longValue() default 7L;
    String stringValue() default "emptystring";
    Class<?> kClassValue() default ParametersTestAnnotation.class;
    TestEnum enumValue() default TestEnum.NONE;
}

public @interface ParametersTestWithNegativeDefaultsAnnotation {
    Byte byteValue() default -2;
    Short shortValue() default -3;
    Double doubleValue() default -4.0;
    Float floatValue() default -5.0f;
    Int intValue() default -6;
    Long longValue() default -7L;
}

public @interface ParameterArraysTestAnnotation {
    boolean[] booleanArrayValue();
    byte[] byteArrayValue() default { };
    short[] shortArrayValue() default { };
    char[] charArrayValue() default { };
    double[] doubleArrayValue() default { };
    float[] floatArrayValue() default { };
    int[] intArrayValue() default { };
    long[] longArrayValue() default { };
    string[] stringArrayValue() default { };
    Class[] kClassArrayValue() default { };
    TestEnum[] enumArrayValue() default { };
}

public enum TestEnum {
    NONE, VALUE1, VALUE2;
}

public @interface InnerAnnotation {
    String value() default "defaultValue";
}

public @interface OuterAnnotation {
    InnerAnnotation innerAnnotation() default InnerAnnotation();
}

/////////////////////////////////////////////////////////
// Tests
/////////////////////////////////////////////////////////

@Test
@Test
public class OnlyTestAnnotation {}

@ParametersTestAnnotation(
    booleanValue = true,
    byteValue = 5,
    shortValue = 202,
    charValue = 'k',
    doubleValue = 5.12,
    floatValue = 123.3f,
    intValue = 2,
    longValue = 4L,
    stringValue = "someValue",
    kClassValue = java.lang.Throwable.class,
    enumValue = TestEnum.VALUE1
)
@Test
public class ParametersTestAnnotationWithValuesTest {}

@ParametersTestAnnotation(
    booleanValue = true,
    byteValue = 5,
    shortValue = 202,
    charValue = 'k',
    doubleValue = 5,
    floatValue = 123,
    intValue = 2,
    longValue = 4,
    stringValue = "someValue",
    kClassValue = java.lang.Throwable.class,
    enumValue = TestEnum.VALUE1
)
@Test
public class ParametersTestAnnotationWithIntegerLiteralValuesTest {}

@ParametersTestAnnotation
@ParametersTestAnnotation
@Test
public class ParametersTestAnnotationWithDefaultsTest {}

@ParametersTestWithNegativeDefaultsAnnotation
@ParametersTestWithNegativeDefaultsAnnotation
@Test
public class ParametersTestWithNegativeDefaultsAnnotationTest {}

@ParameterArraysTestAnnotation(
    booleanArrayValue = {true, false},
    byteArrayValue = {-2, 4},
    shortArrayValue = {-1, 2, 3},
    charArrayValue = {'a', 'b', 'c'},
    doubleArrayValue = {1.1, 2.2, 3.3},
    floatArrayValue = {1.0f, 2.0f, 3.3f},
    intArrayValue = {1, 2, 4, 8, 16},
    longArrayValue = {1L, 2L, 4L, 8L, 16, 32L},
    stringArrayValue = {"first", "second", "third"},
    kClassArrayValue = {java.lang.Throwable.class, ParametersTestAnnotation.class},
    enumArrayValue = {TestEnum.VALUE1, TestEnum.VALUE2, TestEnum.VALUE1, TestEnum.VALUE2}
)
@Test
public class ParameterArraysTestAnnotationWithDefaultTest {}

@ParameterArraysTestAnnotation(
    booleanArrayValue = true,
    byteArrayValue = -2,
    shortArrayValue = -1,
    charArrayValue = 'a',
    doubleArrayValue = 1.1,
    floatArrayValue = 1.0f,
    intArrayValue = 1,
    longArrayValue = 1L,
    stringArrayValue = "first",
    kClassArrayValue = java.lang.Throwable.class,
    enumArrayValue = TestEnum.VALUE1
)
@Test
public class ParameterArraysTestAnnotationWithSingleAsArrayTest {}

@OuterAnnotation(innerAnnotation = @InnerAnnotation(value = "hello from the other side"))
@Test
public class AnnotationWithinAnAnnotationTest {}

// FILE: Annotations.kt
