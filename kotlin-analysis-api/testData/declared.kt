/*
 * Copyright 2022 Google LLC
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
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

// TEST PROCESSOR: DeclaredProcessor
// EXPECTED:
// Base class declared functions:
// subFun
// synthetic constructor for Sub
// Sub class declared functions:
// baseFun
// <init>
// JavaSource class declared functions:
// javaSourceFun
// synthetic constructor for JavaSource
// END
// MODULE: module1
// FILE: lib.kt
open class Base {
    fun baseFun() {}
}
// MODULE: main(module1)
// FILE: sub.kt
class Sub: Base() {
    fun subFun() {}
}

// FILE: JavaSource.java
public class JavaSource {
    public int javaSourceFun() {
        return 1
    }
}
