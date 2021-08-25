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
// KotlinAnnotationLib -> a:debugLibKt,b:defaultInLib
// JavaAnnotationWithDefaults -> stringVal:foo,stringArrayVal:[x, y],typeVal:HashMap<*, *>,typeArrayVal:[LinkedHashMap<*, *>],intVal:3,intArrayVal:[1, 3, 5],enumVal:JavaEnum.DEFAULT,enumArrayVal:[JavaEnum.VAL1, JavaEnum.VAL2],otherAnnotationVal:@OtherAnnotation,otherAnnotationArrayVal:[@OtherAnnotation],kotlinAnnotationLibVal:@OtherKotlinAnnotation
// KotlinAnnotationWithDefaults -> stringVal:foo,stringArrayVal:[x, y],typeVal:HashMap<*, *>,typeArrayVal:[LinkedHashMap<*, *>],intVal:3,intArrayVal:[1, 3, 5],enumVal:JavaEnum.DEFAULT,enumArrayVal:[JavaEnum.VAL1, JavaEnum.VAL2],otherAnnotationVal:@OtherAnnotation,otherAnnotationArrayVal:[@OtherAnnotation],kotlinAnnotationLibVal:@OtherKotlinAnnotation
// KotlinAnnotation -> a:debugJava,b:default
// JavaAnnotation -> debug:debugJava2,withDefaultValue:OK
// JavaAnnotation2 -> y:y-java,x:x-java,z:z-default
// KotlinAnnotation2 -> y:y-java,x:x-java,z:z-default
// END
// MODULE: lib
// FILE: Default.kt
annotation class KotlinAnnotationLib(val a: String, val b: String = "defaultInLib")

annotation class OtherKotlinAnnotation(val b: String = "otherKotlinAnnotationDefault")

// FILE: JavaEnum.java
public enum JavaEnum {
    VAL1,
    VAL2,
    DEFAULT
}

// FILE: OtherAnnotation.java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
@Retention(RetentionPolicy.RUNTIME)
public @interface OtherAnnotation {
    String value();
}

// FILE: JavaAnnotationWithDefaults.java
import java.util.HashMap;
import java.util.LinkedHashMap;
public @interface JavaAnnotationWithDefaults {
    String stringVal() default "foo";
    String[] stringArrayVal() default {"x", "y"};
    Class<?> typeVal() default HashMap.class;
    Class[] typeArrayVal() default {LinkedHashMap.class};
    int intVal() default 3;
    int[] intArrayVal() default {1, 3, 5};
    JavaEnum enumVal() default JavaEnum.DEFAULT;
    JavaEnum[] enumArrayVal() default {JavaEnum.VAL1, JavaEnum.VAL2};
    OtherAnnotation otherAnnotationVal() default @OtherAnnotation("def");
    OtherAnnotation[] otherAnnotationArrayVal() default {@OtherAnnotation("v1")};
    OtherKotlinAnnotation kotlinAnnotationLibVal() default @OtherKotlinAnnotation(b = "JavaAnnotationWithDefaults");
}

// FILE: KotlinAnnotationWithDefaults.kt
import kotlin.reflect.KClass

annotation class KotlinAnnotationWithDefaults(
    val stringVal: String = "foo",
    val stringArrayVal: Array<String> = ["x", "y"],
    val typeVal: KClass<*> = java.util.HashMap::class,
    val typeArrayVal: Array<KClass<*>> = [java.util.LinkedHashMap::class],
    val intVal: Int = 3,
    val intArrayVal: IntArray = [1, 3, 5],
    val enumVal: JavaEnum = JavaEnum.DEFAULT,
    val enumArrayVal: Array<JavaEnum> = [JavaEnum.VAL1, JavaEnum.VAL2],
    val otherAnnotationVal: OtherAnnotation = OtherAnnotation("def"),
    val otherAnnotationArrayVal: Array<OtherAnnotation> = [OtherAnnotation("v1")],
    val kotlinAnnotationLibVal: OtherKotlinAnnotation = OtherKotlinAnnotation("1")
)
// MODULE: main(lib)
// FILE: a.kt

annotation class KotlinAnnotation(val a: String, val b:String = "default")
annotation class KotlinAnnotation2(val x: String, val y:String = "y-default", val z:String = "z-default")

@KotlinAnnotation("debugKt")
@JavaAnnotation("debug")
@JavaAnnotation2(y="y-kotlin", x="x-kotlin")
@KotlinAnnotation2(y="y-kotlin", x="x-kotlin")
@KotlinAnnotationLib("debugLibKt")
@JavaAnnotationWithDefaults
@KotlinAnnotationWithDefaults
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
