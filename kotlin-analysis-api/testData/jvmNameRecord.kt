// TEST PROCESSOR: JvmNameRecordProcessor
// EXPECTED:
// Aliased.z: z
// Couple.first: first
// Couple.second: second
// GenericRecord.g: g
// GenericRecord.h: h
// NamedRecord.id: id
// NamedRecord.name: name
// Single.x: x
// WithBody.a: a
// WithBody.computed: computed
// WithBody.mutable: mutable, mutable
// END
// MODULE: main
// FILE: records.kt
interface Named {
    val name: String
}

interface Generic<A> {
    val g: A
}

@JvmRecord
data class Single(val x: Int)

@JvmRecord
data class Couple(val first: Int, val second: String)

// @JvmRecord classes cannot extend other classes (they extend java.lang.Record),
// so implementing interfaces is the only supertype case.
@JvmRecord
data class NamedRecord(override val name: String, val id: Int) : Named

@JvmRecord
data class GenericRecord<A, B>(override val g: A, val h: B) : Generic<A>

// @JvmRecord constructor properties must be vals; mutable properties are only
// possible in the class body and without a backing field.
@JvmRecord
data class WithBody(val a: Int) {
    val computed: Int
        get() = a * 2
    var mutable: Int
        get() = a
        set(value) {}
}
// FILE: aliased.kt
import kotlin.jvm.JvmRecord as JR

@JR
data class Aliased(val z: Int)
