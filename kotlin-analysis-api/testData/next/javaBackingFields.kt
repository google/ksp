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

// TEST PROCESSOR: JavaBackingFieldProcessor
// EXPECTED:
// lib.Fields
// - lib.Fields.x: kotlin.Int - [PRIVATE, FINAL]
// - lib.Fields.x.field: kotlin.Int - [PRIVATE, FINAL]
// - lib.Fields.y: kotlin.Int - [PUBLIC, FINAL]
// - lib.Fields.y.field: kotlin.Int - [PUBLIC, FINAL]
// - lib.Fields.z: kotlin.Int - [PROTECTED, FINAL]
// - lib.Fields.z.field: kotlin.Int - [PROTECTED, FINAL]
// - lib.Fields.w: kotlin.Int - [FINAL]
// - lib.Fields.w.field: kotlin.Int - [FINAL]
// lib.AccessorsOnly
// lib.ObscureFields
// - lib.ObscureFields.back: kotlin.Int - [PRIVATE, FINAL]
// - lib.ObscureFields.back.field: kotlin.Int - [PRIVATE, FINAL]
// SourceFields
// - SourceFields.x: kotlin.Int - [PRIVATE]
// - SourceFields.x.field: kotlin.Int - [PRIVATE]
// - SourceFields.y: kotlin.Int - [PUBLIC]
// - SourceFields.y.field: kotlin.Int - [PUBLIC]
// - SourceFields.z: kotlin.Int - [PROTECTED]
// - SourceFields.z.field: kotlin.Int - [PROTECTED]
// - SourceFields.w: kotlin.Int - []
// - SourceFields.w.field: kotlin.Int - []
// SourceAccessorsOnly
// SourceObscureFields
// - SourceObscureFields.back: kotlin.Int - [PRIVATE]
// - SourceObscureFields.back.field: kotlin.Int - [PRIVATE]
// - SourceObscureFields.unused: kotlin.Boolean - [PRIVATE, FINAL]
// - SourceObscureFields.unused.field: kotlin.Boolean - [PRIVATE, FINAL]
// END

// MODULE: lib
// FILE: Fields.java
package lib;

public class Fields {
    private int x;
    public int y;
    protected int z;
    int w;
}

// FILE: AccessorsOnly.java
package lib;

public class AccessorsOnly {
    public int getX() { return 42; }
    public int getY() { return 42; }
    public void setY() { }
}

// FILE: ObscureFields.java
package lib;

public class ObscureFields {
    private int back = 0;

    public int getFlag1() {
        return back & 1;
    }

    public void setFlag1(boolean flag) {
        if (flag) {
            back |= 1;
        } else {
            back &= ~1;
        }
    }
}

// MODULE: main(lib)
// FILE: SourceFields.java
public class SourceFields {
    private int x;
    public int y;
    protected int z;
    int w;
}

// FILE: SourceAccessorsOnly.java
public class SourceAccessorsOnly {
    public int getX() { return 42; }
    public int getY() { return 42; }
    public void setY() {}
}

// FILE: SourceObscureFields.java

public class SourceObscureFields {
    private int back = 0;
    private final boolean unused = false;

    public int getFlag1() {
        return back & 1;
    }

    public void setFlag1(boolean flag) {
        if (flag) {
            back |= 1;
        } else {
            back &= ~1;
        }
    }
}
