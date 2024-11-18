// TEST PROCESSOR: JvmNameProcessor
// EXPECTED:
// (getX, setX), (getY, null), (isOpen, setOpen)
// (isOpen, setOpen), (getX, setX), (getY, null)
// stringParameter
// stringParameter
// stringParameter
// JvmName: stringParameter
// JvmName: stringParameter
// END
// MODULE: lib
// FILE: Lib.kt
data class TestLibDataClass(var x: Int, val y: String, var isOpen: String)
// FILE: MyAnnotationLib.kt
annotation class MyAnnotationLib(
    @get:JvmName("stringParameter")
    val stringParam: String
)
// FILE: MyAnnotationUserLib.java
@MyAnnotationLib(stringParameter = "foo")
class MyAnnotationUserLib {}

// MODULE: main(lib)
// FILE: K.kt
// FILE: MyAnnotation.kt
annotation class MyAnnotation(
    @get:JvmName("stringParameter")
    val stringParam: String
)
data class TestDataClass(var x: Int, val y: String, var isOpen: String)
// FILE: MyAnnotationUser.java
@MyAnnotationLib(stringParameter = "foo")
@MyAnnotation(stringParameter = "foo")
class MyAnnotationUser {}

