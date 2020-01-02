// WITH_RUNTIME
// TEST PROCESSOR: AnnotationArgumentProcessor
// EXPECTED:
// Str
// 42
// NormalClass(value=/Foo)
// END

class Foo

annotation class Bar(
    val argStr: String,
    val argInt: Int,
    val argCls: kotlin.reflect.KClass<*>,
    val argDef: Int = 31
)

@Bar("Str", 42, Foo::class)
class SomeClass
