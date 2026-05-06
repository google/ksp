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

// TEST PROCESSOR: EnumModifierProcessor
// PROCESSOR INPUT: MyLibEnum, MySrcEnum, MyLibEnum.ONE, MySrcEnum.FOUR
// EXPECTED:
// MyLibEnum: ACC_ENUM
// MyLibEnum: ACC_FINAL
// MyLibEnum: ACC_PUBLIC
// MyLibEnum: ACC_SUPER
// MyLibEnum.ONE: ACC_ENUM
// MyLibEnum.ONE: ACC_FINAL
// MyLibEnum.ONE: ACC_PUBLIC
// MyLibEnum.ONE: ACC_STATIC
// MySrcEnum: ACC_ENUM
// MySrcEnum: ACC_FINAL
// MySrcEnum: ACC_PUBLIC
// MySrcEnum: ACC_SUPER
// MySrcEnum.FOUR: ACC_ENUM
// MySrcEnum.FOUR: ACC_FINAL
// MySrcEnum.FOUR: ACC_PUBLIC
// MySrcEnum.FOUR: ACC_STATIC
// END

// MODULE: lib
// FILE: MyLibEnum.kt

enum class MyLibEnum {
    ONE,
    TWO,
    THREE
}

// MODULE: main(lib)
// FILE: MySrcEnum.kt

enum class MySrcEnum {
    FOUR,
    FIVE,
    SIX
}
