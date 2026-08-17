// TEST PROCESSOR: DefaultKClassValueProcessor
// EXPECTED:
// kotlin.String
// kotlin.String
// kotlin.String
// kotlin.Int
// END
// MODULE: lib1
// FILE: lib1.kt
annotation class ExampleAnnotation(val value: kotlin.reflect.KClass<*> = java.lang.String::class)

// MODULE: main(lib1)
// FILE: a.kt

@ExampleAnnotation(String::class)
class Example

@ExampleAnnotation(Int::class)
class Example2

