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

// TEST PROCESSOR: FooBarProcessor
// PROCESSOR INPUT: com.example.lib.BarAnno
// EXPECTED:
// com.example.lib.BarAnno: Foo
// Arg: value = <ERROR TYPE>
// PluginProblemReporter: registered, created PluginException
// END

// MODULE: lib
// FILE: com/example/lib/lib.kt
package com.example.lib

// FILE: com/example/lib/FooEnum.java
package com.example.lib;

public enum FooEnum {
    FOO_VAL1,
    FOO_VAL2
}

// FILE: com/example/lib/FooAnno.java
package com.example.lib;

public @interface FooAnno {
    FooEnum value() default FooEnum.FOO_VAL1;
}

// FILE: com/example/lib/BarAnno.java
package com.example.lib;

public @interface BarAnno {
}

// MODULE: main(lib)
// FILE: com/example/main/main.kt
package com.example.main

// FILE: com/example/main/Foo.java
package com.example.main;

import com.example.lib.BarAnno;
import com.example.lib.FooAnno;
import com.example.lib.FooEnum;

@BarAnno
public class Foo {
    public Foo(@FooAnno(FooEnum.DEPRECATED_VALUE) int x) {
    }
}
