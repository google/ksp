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

annotation class KotlinAnnotation(val a: String, val b:String = "default")
annotation class KotlinAnnotation2(val x: String, val y:String = "y-default", val z:String = "z-default")

@KotlinAnnotation("debugKt")
@JavaAnnotation("debug")
@JavaAnnotation2(y="y-kotlin", x="x-kotlin")
@KotlinAnnotation2(y="y-kotlin", x="x-kotlin")
class A

// FILE: JavaAnnotation.java
public @interface JavaAnnotation {
    String debug();
    String withDefaultValue()  default "OK";
}

// FILE: JavaAnnotation2.java
public @interface JavaAnnotation2 {
    String x() default "x-default";
    String y() default "y-default";
    String z() default "z-default";
}

// FILE: JavaAnnotated.java

@KotlinAnnotation("debugJava")
@JavaAnnotation("debugJava2")
@JavaAnnotation2(y="y-java", x="x-java")
@KotlinAnnotation2(y="y-java", x="x-java")
public class JavaAnnotated {

}
