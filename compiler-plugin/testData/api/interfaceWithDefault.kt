/*
 * Copyright 2010-2020 Google LLC, JetBrains s.r.o.
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

// TEST PROCESSOR: DefaultFunctionProcessor
// EXPECTED:
// funLiteral: false
// funWithBody: false
// emptyFun: true
// foo: false
// bar: true
// iterator: true
// equals: false
// interfaceProperty: isAbstract: true: isMutable: false
// interfaceVar: isAbstract: true: isMutable: true
// nonAbstractInterfaceProp: isAbstract: false: isMutable: false
// B: true
// parameterVal: isAbstract: false: isMutable: false
// parameterVar: isAbstract: false: isMutable: true
// abstractVar: isAbstract: true: isMutable: true
// abstractProperty: isAbstract: true: isMutable: false
// a: false
// END
// FILE: a.kt
interface KTInterface: Sequence<String> {
    fun funLiteral() = 1

    fun funWithBody(): Int {
        return 1
    }

    fun emptyFun()

    val interfaceProperty: String

    var interfaceVar: Int

    val nonAbstractInterfaceProp: Int
    get() = 1
}

abstract class B(val parameterVal: String, var parameterVar: String) {
    abstract var abstractVar: String
    abstract val abstractProperty: String
    val a: String = "str"
}

// FILE: C.java
interface C {
    default int foo() {
        return 1;
    }

    int bar()
}
