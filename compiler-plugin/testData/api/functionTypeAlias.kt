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
// TEST PROCESSOR: FunctionTypeAliasProcessor
// EXPECTED:
// Function1<String, String> ?= Function1<String, String> : true / true
// Function1<String, String> ?= String : false / false
// Function1<String, String> ?= [typealias F] : false / false
// Function1<String, String> ?= [typealias Foo] : true / true
// String ?= Function1<String, String> : false / false
// String ?= String : true / true
// String ?= [typealias F] : true / true
// String ?= [typealias Foo] : false / false
// [typealias F] ?= Function1<String, String> : false / false
// [typealias F] ?= String : true / true
// [typealias F] ?= [typealias F] : true / true
// [typealias F] ?= [typealias Foo] : false / false
// [typealias Foo] ?= Function1<String, String> : true / true
// [typealias Foo] ?= String : false / false
// [typealias Foo] ?= [typealias F] : false / false
// [typealias Foo] ?= [typealias Foo] : true / true
// END

typealias F = String
val y: Foo = { it }
typealias Foo = (F) -> String
