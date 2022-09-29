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
// TEST PROCESSOR: TypeAliasComparisonProcessor
// EXPECTED:
// String = String : true
// String = String : true
// String = String : true
// String = String : true
// String = [@Anno] [typealias F] : true
// String = [@Anno] [typealias F] : true
// String = [@Bnno] [typealias F] : true
// String = [@Bnno] [typealias F] : true
// [@Anno] [typealias F] = String : true
// [@Anno] [typealias F] = String : true
// [@Anno] [typealias F] = [@Anno] [typealias F] : true
// [@Anno] [typealias F] = [@Bnno] [typealias F] : true
// [@Bnno] [typealias F] = String : true
// [@Bnno] [typealias F] = String : true
// [@Bnno] [typealias F] = [@Anno] [typealias F] : true
// [@Bnno] [typealias F] = [@Bnno] [typealias F] : true
// END

annotation class Anno
annotation class Bnno

typealias F = String
typealias Foo = (@Anno F) -> String

fun bar(arg: @Bnno F) {
}
