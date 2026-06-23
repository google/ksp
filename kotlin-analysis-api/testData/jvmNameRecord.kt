/*
 * Copyright 2026 Google LLC
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
// TEST PROCESSOR: JvmNameRecordProcessor
// EXPECTED:
// Aliased.z: z
// Couple.first: first
// Couple.second: second
// GenericRecord.g: g
// GenericRecord.h: h
// LibRecord.w: w
// NamedRecord.id: id
// NamedRecord.name: name
// Single.x: x
// TypeAliased.t: t
// WithBody.a: a
// WithBody.computed: computed
// WithBody.mutable: mutable, mutable(value)
// WithJvmName.n: customName
// WithRecordProp.inner: inner
// WithRecordProp.n: n
// END
// JVM_TARGET: 17
// MODULE: lib
// FILE: LibRecord.kt
@JvmRecord
data class LibRecord(val w: Int)
// MODULE: main(lib)
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

// An explicit @JvmName takes precedence over the record accessor naming.
@JvmRecord
data class WithJvmName(@get:JvmName("customName") val n: Int)

// A property whose type is also a @JvmRecord — the property type does not affect accessor naming.
@JvmRecord
data class WithRecordProp(val inner: Single, val n: Int)
// FILE: aliased.kt
import kotlin.jvm.JvmRecord as JR

@JR
data class Aliased(val z: Int)
// FILE: typealiased.kt
import kotlin.jvm.JvmRecord

typealias JvmRec = JvmRecord

@JvmRec
data class TypeAliased(val t: Int)
