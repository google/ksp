/*
 * Copyright 2021 Google LLC
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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


// TEST PROCESSOR: ThrowListProcessor
// EXPECTED:
// java.io.IOException,java.util.NoSuchElementException
// java.io.IOException,java.lang.IndexOutOfBoundsException
// java.io.IOException,java.util.NoSuchElementException
// java.lang.IndexOutOfBoundsException
// END
// FILE: ThrowsException.java
import java.io.IOException;
import java.lang.IndexOutOfBoundsException;

public class ThrowsException {
    public int foo() throws IOException, IndexOutOfBoundsException{
        return 1;
    }
}
// FILE: a.kt
class ThrowsKt {
    @Throws(java.io.IOException::class, java.util.NoSuchElementException::class)
    fun throwsKT()

    @set:Throws(java.lang.IndexOutOfBoundsException::class)
    var a: Int
    @Throws(java.io.IOException::class, java.util.NoSuchElementException::class)
    get() {
        return 1
    }
    set(a: Int) {

    }
}
