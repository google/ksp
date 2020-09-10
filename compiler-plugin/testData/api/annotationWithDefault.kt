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