// TEST PROCESSOR: JvmNameProcessor
// EXPECTED:
// (getX, setX), (getY, null)
// (getX, setX), (getY, null)
// stringParameter
// stringParameter
// END
// MODULE: lib
// FILE: Lib.kt
data class TestLibDataClass(var x: Int, val y: String)
// FILE: MyAnnotation.kt
annotation class MyAnnotation(
    @get:JvmName("stringParameter")
    val stringParam: String
)
// FILE: MyAnnotationUserLib.java
@MyAnnotation(stringParameter = "foo")
class MyAnnotationUserLib {}

// MODULE: main(lib)
// FILE: K.kt
data class TestDataClass(var x: Int, val y: String)
// FILE: MyAnnotationUser.java
@MyAnnotation(stringParameter = "foo")
class MyAnnotationUser {}

