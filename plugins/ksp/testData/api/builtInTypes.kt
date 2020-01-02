// WITH_RUNTIME
// TEST PROCESSOR: BuiltInTypesProcessor
// EXPECTED:
// Annotation: OK
// Any: OK
// Array<*>: OK
// Boolean: OK
// Byte: OK
// Char: OK
// Double: OK
// Float: OK
// Int: OK
// Iterable<*>: OK
// Long: OK
// Nothing: OK
// Number: OK
// Short: OK
// String: OK
// Unit: OK
// END

val a: Any = 0
val b: Unit = Unit
val c: Number = 0
val d: Byte = 0
val e: Short = 0
val f: Int = 0
val g: Long = 0
val h: Float = 0.0f
val i: Double = 0.0
val j: Char = '0'
val k: Boolean = false
val l: String = ""
val m: Iterable<*> = listOf<Any>()
val n: Annotation = object: Annotation {}
fun foo(): Nothing = throw Error()
val o: Array<*> = arrayOf<Any>()
