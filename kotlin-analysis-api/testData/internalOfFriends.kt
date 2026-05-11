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

// WITH_RUNTIME
// TEST PROCESSOR: InternalOfFriendsProcessor
// EXPECTED:
// fun1: MyClass0: MyClass0
// fun2: <ERROR TYPE: hasInvoke1>: <ERROR TYPE: hasInvoke1>
// fun3: MyClass2: MyClass2
// fun4: MyClass3: MyClass3
// END

// MODULE: lib-1
class MyClass0
class MyClass1
class MyClass2
class MyClass3

// MODULE: lib-regular(lib-1)
internal class HasInvoke1 {
    operator fun invoke() = MyClass1()
}

// MODULE: lib-friend(lib-1)
internal class HasInvoke2 {
    operator fun invoke() = MyClass2()
}

// MODULE: main(lib-regular, lib-1)(lib-friend)
internal class HasInvoke3 {
    operator fun invoke() = MyClass3()
}

fun fun1() = MyClass0()
fun fun2(hasInvoke1: HasInvoke1) = hasInvoke1()
fun fun3(hasInvoke2: HasInvoke2) = hasInvoke2()
fun fun4(hasInvoke3: HasInvoke3) = hasInvoke3()
