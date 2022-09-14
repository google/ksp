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
// TEST PROCESSOR: AnnotationJavaTypeValueProcessor
// EXPECTED:
// JavaAnnotated
// JavaAnnotation ->
// primitives = [Character, Boolean, Byte, Short, Integer, Long, Float, Double]
// objects = [Character, Boolean, Byte, Short, Integer, Long, Float, Double]
// primitiveArrays = [Array<Character>, Array<Boolean>, Array<Byte>, Array<Short>, Array<Integer>, Array<Long>, Array<Float>, Array<Double>]
// objectArrays = [Array<Character>, Array<Boolean>, Array<Byte>, Array<Short>, Array<Integer>, Array<Long>, Array<Float>, Array<Double>, Array<String>, Array<Object>]
// END
// FILE: a.kt


// FILE: JavaAnnotation.java

public @ interface JavaAnnotation {
    Class[] primitives(); // PsiPrimitiveType
    Class[] objects(); // PsiType
    Class[] primitiveArrays(); // PsiArrayType
    Class[] objectArrays(); // PsiArrayType
}

// FILE: JavaAnnotated.java

import java.util.*;

@JavaAnnotation(
    primitives = { char.class, boolean .class, byte.class, short.class, int.class, long.class, float.class, double.class },
    objects = { Character.class, Boolean .class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class },
    primitiveArrays = { char[].class, boolean [].class, byte[].class, short[].class, int[].class, long[].class, float[].class, double[].class },
    objectArrays = { Character[].class, Boolean [].class, Byte[].class, Short[].class, Integer[].class, Long[].class, Float[].class, Double[].class, String[].class, Object[].class }
)
public class JavaAnnotated {
}
