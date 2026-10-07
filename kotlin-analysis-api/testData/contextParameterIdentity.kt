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

// A KSContextParameter must be canonical: exactly one instance per source context parameter,
// regardless of whether it was produced by the declaration tree, by getSymbolsWithAnnotation, or by
// restoring a deferred symbol in a later round. `distinctInstances=1` asserts that invariant;
// `distinctInstances=2` means two producers disagreed and Set/distinct() based de-duplication in
// the annotation resolution strategies silently stops working.

// TEST PROCESSOR: ContextParameterIdentityProcessor
// PROCESSOR INPUT: Anno
// EXPECTED:
// EXPECT CURRENT: round0 reportedContextParameters=0
// EXPECT NEXT: round0 reportedContextParameters=3
// EXPECT NEXT: round0 MyClass.myProp.ctxParam1 canonical=true distinctInstances=1
// EXPECT NEXT: round0 bar.ctxParam1 canonical=true distinctInstances=1
// EXPECT NEXT: round0 foo.ctxParam1 canonical=true distinctInstances=1
// EXPECT CURRENT: round0 non-annotated context parameters 0: []
// EXPECT NEXT: round0 non-annotated context parameters 1: [bar.ctxParam2]
// EXPECT CURRENT: round1 reportedContextParameters=0
// EXPECT NEXT: round1 reportedContextParameters=3
// EXPECT NEXT: round1 MyClass.myProp.ctxParam1 canonical=true distinctInstances=1
// EXPECT NEXT: round1 bar.ctxParam1 canonical=true distinctInstances=1
// EXPECT NEXT: round1 foo.ctxParam1 canonical=true distinctInstances=1
// EXPECT CURRENT: round1 non-annotated context parameters 0: []
// EXPECT NEXT: round1 non-annotated context parameters 1: [bar.ctxParam2]
// END

// FILE: Main.kt

annotation class Anno

interface Ctx1
interface Ctx2

context(@Anno ctxParam1: Ctx1)
fun foo() {
}

// ctxParam2 is deliberately not annotated: it must never show up in the reported symbols, but it
// still has to be reachable from the declaration tree.
context(@Anno ctxParam1: Ctx1, ctxParam2: Ctx2)
fun bar() {
}

class MyClass {
    context(@Anno ctxParam1: Ctx1)
    val myProp: String get() = ""
}
