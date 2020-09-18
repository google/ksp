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
// KotlinAnnotation -> a:debugJava,b:default
// JavaAnnotation -> debug:debugJava2,withDefaultValue:OK
// END
// FILE: a.kt

annotation class KotlinAnnotation(val a: String, val b:String = "default")

@KotlinAnnotation("debugKt")
@JavaAnnotation("debug")
class A

// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    String debug();
    String withDefaultValue()  default "OK";
}

// FILE: JavaAnnotated.java

@KotlinAnnotation("debugJava")
@JavaAnnotation("debugJava2")
public class JavaAnnotated {

}
