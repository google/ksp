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

// TEST PROCESSOR: CheckOverrideProcessor
// EXPECTED:
// KotlinList.get overrides JavaList.get: false
// KotlinList.foo overrides JavaList.foo: true
// KotlinList.fooo overrides JavaList.foo: false
// KotlinList.equals overrides JavaList.equals: true
// KotlinList2.baz overrides KotlinList.baz: true
// KotlinList2.baz overrides KotlinList.bazz: false
// KotlinList2.bazz overrides KotlinList.bazz: true
// KotlinList2.bazz overrides KotlinList.baz: false
// END
// FILE: a.kt

annotation class GetAnno
annotation class FooAnno
annotation class BarAnno
annotation class BazAnno
annotation class Baz2Anno
annotation class BazzAnno
annotation class Bazz2Anno

open class KotlinList(): JavaList() {
    @GetAnno
    fun get(): Double {
        return 2.0
    }

    override fun equals(other: Any?): Boolean {
        return false
    }

    @FooAnno
    override fun foo(): Int {
        return 2
    }

    @BarAnno
    override fun fooo(): Int {
        return 2
    }

    @Baz2Anno
    open val baz: Int get() {
        return 1
    }

    @Bazz2Anno
    open val bazz: Int get() {
        return 1
    }
}

class KotlinList2(@BazzAnno override val bazz: Int = 2): KotlinList() {
    @BazAnno
    override val baz: Int get() {
        return 2
    }
}

// FILE: JavaList.java

import java.util.*;

public class JavaList extends List<String> {
    @Override
    public String get(int index) {
        return "OK";
    }

    protected int foo() {
        return 1;
    }
}
