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

// TEST PROCESSOR: AnnotationArrayValueProcessor
// EXPECTED:
// KotlinAnnotated
// KotlinAnnotation ->
// stringArray: Array = [a, b, null, c]
// classArray: Array = [Any, List<*>]
// JavaAnnotation ->
// stringArray: Array = [x, y, null, z]
// classArray: Array = [String, Long]
// JavaAnnotated
// KotlinAnnotation ->
// stringArray: Array = [j-a, j-b, null, j-c]
// classArray: Array = [Object, List<*>]
// JavaAnnotation ->
// stringArray: Array = [j-x, j-y, null, j-z]
// classArray: Array = [Integer, Character]
// END
// FILE: a.kt

annotation class KotlinAnnotation(val stringArray: Array<String?>, val classArray: Array<KClass<*>?>)

@KotlinAnnotation(
    stringArray = ["a", "b", null, "c"],
    classArray = [Any::class, List::class]
)
@JavaAnnotation(
    stringArray = ["x", "y", null, "z"],
    classArray = [String::class, Long::class]
)
class KotlinAnnotated

// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    String[] stringArray();
    Class[] classArray();
}

// FILE: JavaAnnotated.java
import java.util.*;
@KotlinAnnotation(
    stringArray = {"j-a", "j-b", null, "j-c"},
    classArray = {Object.class, List.class}
)
@JavaAnnotation(
    stringArray = {"j-x", "j-y", null, "j-z"},
    classArray = {Integer.class, Character.class}
)
public class JavaAnnotated {
}