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

// TEST PROCESSOR: InheritedTypeAliasProcessor
// EXPECTED:
// sub: arg :INVARIANT Float
// sub: prop :INVARIANT Int
// super: arg :INVARIANT Float
// super: prop :INVARIANT Int
// END

typealias AliasMap<T> = Map<String, T>
typealias AliasFun<T> = (T) -> Double


interface Sub : Super


interface Super {
    val prop: AliasMap<Int>
    fun foo(arg: AliasFun<Float>)
}
