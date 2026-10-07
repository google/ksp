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

// A dangling `context(...)` modifier list (one not followed by a declaration) is a syntax error,
// but KSP must degrade gracefully on erroneous code instead of throwing an InternalKSPException.
// The Analysis API reports the containing symbol of such a context parameter as the enclosing
// class, or as the KaFileSymbol when it is top-level. Since a dangling modifier list is not a
// declaration, AAResolutionStrategy never visits it, so for parity neither strategy reports the
// annotated context parameters inside it.

// TEST PROCESSOR: GetSymbolsWithAnnotationProcessor
// PROCESSOR INPUT: Anno
// EXPECTED:
// Anno: annotatedFun
// EXPECT NEXT: Anno: foo.ctxParam
// END

// FILE: Main.kt

annotation class Anno

interface Ctx

@Anno
fun annotatedFun() {
}

context(@Anno ctxParam: Ctx)
fun foo() {
}

class MyClass {
    context(@Anno danglingInClass: Ctx)
}

// FILE: Dangling.kt

context(@Anno danglingTopLevel: Ctx)
