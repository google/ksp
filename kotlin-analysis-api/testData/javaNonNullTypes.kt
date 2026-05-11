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

// WITH_RUNTIME
// TEST PROCESSOR: JavaNonNullProcessor
// EXPECTED:
// javaNotNullFieldRef: NOT_NULL
// javaNullableFieldRef: NULLABLE
// javaBothFieldRef: PLATFORM
// javaNoneFieldRef: NULLABLE
// bothField: PLATFORM
// nullableField: NULLABLE
// notNullField: NOT_NULL
// noneField: PLATFORM
// notNullFun: NOT_NULL
// nullableParam: NULLABLE
// END
// MODULE: lib
// FILE: dummy.kt
class dummy

// FILE: org/jetbrains/annotations/NotNull.java
package org.jetbrains.annotations;

public @interface NotNull {

}

// FILE: org/jetbrains/annotations/Nullable.java
package org.jetbrains.annotations;

public @interface Nullable {

}

// MODULE: main(lib)
// FILE: a.kt
val javaNotNullFieldRef = JavaNonNull().notNullField
val javaNullableFieldRef = JavaNonNull().nullableField
val javaBothFieldRef = JavaNonNull().bothField
val javaNoneFieldRef = JavaNonNull().nonField


// FILE: JavaNonNull.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class JavaNonNull {
    @Nullable
    @NotNull
    public String bothField;

    @Nullable
    public String nullableField;

    @NotNull
    public String notNullField;

    public String noneField;

    @NotNull
    public String notNullFun(@Nullable String nullableParam) {}
}
