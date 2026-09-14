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

// TARGET_BACKEND: NATIVE
// WITH_FIXED_TARGET: linux_x64
// TEST PROCESSOR: NativePackageDeclarationProcessor
// PROCESSOR INPUT: com.example.test, com.example.app
// EXPECTED:
// com.example.test.LibDeclaration
// com.example.app.Foo
// END

// MODULE: lib
// FILE: LibDeclaration.kt

package com.example.test

class LibDeclaration

// MODULE: main(lib)
// FILE: Foo.kt

package com.example.app

import com.example.test.LibDeclaration

class Foo(val decl: LibDeclaration)
