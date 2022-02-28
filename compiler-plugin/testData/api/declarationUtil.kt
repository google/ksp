/*
 * Copyright 2020 Google LLC
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: DeclarationUtilProcessor
// FORMAT: <name>: isInternal: isLocal: isPrivate: isProtected: isPublic: isOpen
// EXPECTED:
// Cls: internal
// Cls / <init>: public
// Cls.b: public open
// Cls / <init>: public
// Cls / <init> / aaa: local
// Cls.prop: public open
// Cls.protectedProp: protected open
// Cls.abstractITFFun: public open
// Cls.pri: private
// ITF: public open
// ITF.prop: public open
// ITF.protectedProp: protected open
// ITF.b: public open
// ITF.abstractITFFun: public open
// ITF.nonAbstractITFFun: public open
// ITF.nonAbstractITFFun / aa: local
// NestedClassSubjects: public open
// NestedClassSubjects.NestedDataClass: public
// NestedClassSubjects.NestedDataClass / <init>: public
// NestedClassSubjects.NestedDataClass.field: public
// NestedClassSubjects.NestedFinalClass: public
// NestedClassSubjects.NestedFinalClass / <init>: public
// NestedClassSubjects.NestedFinalClass.field: public
// NestedClassSubjects.NestedOpenClass: public open
// NestedClassSubjects.NestedOpenClass / <init>: public
// NestedClassSubjects.NestedOpenClass.field: public
// NestedClassSubjects.NestedInterface: public open
// END
// FILE: a.kt
internal class Cls(override val b: Int) : ITF {
    constructor() {
        val aaa = 2
        Cls(aaa)
    }
    override val prop: Int = 2

    override val protectedProp: Int = 2

    override fun abstractITFFun(): Int {
        return 2
    }

    private val pri: Int = 3
}

interface ITF {
    val prop: Int

    protected val protectedProp: Int

    val b: Int = 1

    fun abstractITFFun(): Int

    fun nonAbstractITFFun(): Int {
        val aa = "local"
        return 1
    }
}

interface NestedClassSubjects {
    data class NestedDataClass(
        val field: String,
    )
    class NestedFinalClass(
        val field: String,
    )
    open class NestedOpenClass(
        val field: String,
    )
    interface NestedInterface
}
