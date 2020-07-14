// WITH_RUNTIME
// TEST PROCESSOR: AnnotationArgumentProcessor
// EXPECTED:
// Str
// 42
// Foo
// File
// Local
// @Foo
// @Suppress
// G
// 31
// END

enum class RGB {
    R, G, B
}

annotation class Foo(val s: Int)

annotation class Bar(
    val argStr: String,
    val argInt: Int,
    val argClsUser: kotlin.reflect.KClass<*>,
    val argClsLib: kotlin.reflect.KClass<*>,
    val argClsLocal: kotlin.reflect.KClass<*>,
    val argAnnoUser: Foo,
    val argAnnoLib: Suppress,
    val argEnum: RGB,
    val argDef: Int = 31
)

fun Fun() {
    @Bar("Str", 42, Foo::class, java.io.File::class, Local::class, Foo(17), Suppress("name1", "name2"), RGB.G)
    class Local
}
