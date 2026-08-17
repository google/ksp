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

// TEST PROCESSOR: GetSymbolsWithAnnotationProcessor
// PROCESSOR INPUT: Anno
// EXPECTED:
// Anno: MyClass.myImmProp
// Anno: MyClass.myImmProp.ctxParam1
// Anno: MyClass.myMutProp
// Anno: MyClass.myMutProp.ctxParam1
// Anno: MyClass.myMutProp.ctxParam2
// Anno: MyClass.onlyOneCtxParamAnnotated.ctxParam2
// Anno: bar.ctxParam1
// Anno: bar.ctxParam2
// Anno: bar.str
// Anno: foo.ctxParam1
// END

// FILE: Main.kt

annotation class Anno

interface CtxParam1
interface CtxParam2

context(@Anno ctxParam1: CtxParam1)
fun foo() {
}

context(@Anno ctxParam1: CtxParam1, @Anno ctxParam2: CtxParam2)
fun bar(@Anno str: String) {
}

class MyClass {
    context(@Anno ctxParam1: CtxParam1)
    @Anno
    val myImmProp: String get() = ""

    context(@Anno ctxParam1: CtxParam1, @Anno ctxParam2: CtxParam2)
    @Anno
    var myMutProp: String get() = ""

    context(ctxParam1: CtxParam1, @Anno ctxParam2: CtxParam2)
    fun onlyOneCtxParamAnnotated() {}
}
