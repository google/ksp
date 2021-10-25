// WITH_RUNTIME
// TEST PROCESSOR: ValidateProcessor
// EXPECTED:
// ErrorInMember invalid
// goodProp valid
// badProp invalid
// errorFun invalid
// <init> valid
// SkipErrorInMember valid
// skipProp valid
// skipFun valid
// <init> valid
// GoodClass valid
// C valid
// BadJavaClass invalid
// ErrorAnnotationType invalid
// ErrorInAnnotationArgumentSingleType invalid
// ErrorInAnnotationArgumentMultipleTypes invalid
// ErrorInAnnotationArgumentComposed invalid
// ValidAnnotationArgumentType valid
// END
// FILE: a.kt
annotation class Anno(val i: Int)

annotation class AnnoWithTypes(
    val type: KClass<*> = Any::class,
    val types: Array<KClass<*>> = []
)

annotation class AnnoComposed(
    val composed: AnnoWithTypes
)

@Anno(1)
class ErrorInMember : C {
    val goodProp: Int
    val badProp: () -> NonExistType
    fun errorFun(): NonExistType {

    }
}

class SkipErrorInMember {
    val skipProp: NonExistType
    fun skipFun(): NonExitType {

    }
}

@NonExistAnnotation
class ErrorAnnotationType {
}

@Anno(1)
open class GoodClass {
    val a: Int = 1

    fun foo(): Int = 1

    fun bar() {
        val x = a
    }
}

@AnnoWithTypes(type = NonExistType::class)
class ErrorInAnnotationArgumentSingleType {

}

@AnnoWithTypes(types = [ GoodClass::class, NonExistType::class ])
class ErrorInAnnotationArgumentMultipleTypes {

}

@AnnoComposed(composed = AnnoWithTypes(type = NonExistType::class))
class ErrorInAnnotationArgumentComposed {

}

@AnnoWithTypes(type = GoodClass::class)
class ValidAnnotationArgumentType {

}

// FILE: C.java

public class C extends GoodClass {}

class BadJavaClass extends NonExistType {

}