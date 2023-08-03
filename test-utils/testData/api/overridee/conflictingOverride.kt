/*
 * Copyright 2023 Google LLC
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// WITH_RUNTIME
// TEST PROCESSOR: OverrideeProcessor
// EXPECTED:
// ConflictingSubject1:
// ConflictingSubject1.absFoo() -> MyInterface.absFoo()
// ConflictingSubject2:
// ConflictingSubject2.absFoo() -> MyAbstract.absFoo()
// ConflictingSubject3:
// ConflictingSubject3.absFoo() -> MyInterface.absFoo()
// ConflictingSubject4:
// ConflictingSubject4.absFoo() -> MyInterface2.absFoo()
// END

// FILE: conflictingOverrides.kt
interface MyInterface {
    fun absFoo(): Unit
}

interface MyInterface2 {
    fun absFoo(): Unit
}

abstract class MyAbstract: MyInterface {
    override fun absFoo(): Unit {val a = 1}
}

class ConflictingSubject1: MyInterface, MyAbstract() {
    override fun absFoo(): Unit = TODO()
}

class ConflictingSubject2: MyAbstract(), MyInterface {
    override fun absFoo(): Unit = TODO()
}

class ConflictingSubject3: MyInterface, MyInterface2 {
    override fun absFoo(): Unit = TODO()
}

class ConflictingSubject4: MyInterface2, MyInterface {
    override fun absFoo(): Unit = TODO()
}
