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
// KotlinList.foo overrides KotlinList.foo: false
// KotlinList.equals overrides JavaList.equals: true
// KotlinList2.baz overrides KotlinList.baz: true
// KotlinList2.baz overrides KotlinList.bazz: false
// KotlinList2.bazz overrides KotlinList.bazz: true
// KotlinList2.bazz overrides KotlinList.baz: false
// KotlinList2.baz overrides KotlinList2.baz: false
// JavaImpl.getY overrides JavaImpl.getX: false
// JavaImpl.getY overrides MyInterface.x: false
// JavaImpl.getX overrides MyInterface.x: true
// JavaImpl.setY overrides MyInterface.y: true
// JavaImpl.setX overrides MyInterface.x: false
// JavaImpl.getY overrides JavaImpl.getY: false
// MyInterface.x overrides JavaImpl.getY: false
// MyInterface.x overrides JavaImpl.getX: false
// MyInterface.y overrides JavaImpl.setY: false
// MyInterface.y overrides MyInterface.y: false
// JavaDifferentReturnType.foo overrides JavaList.foo: true
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

interface MyInterface {
    val x: Int
    var y: Int
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

// FILE: JavaImpl.java

public class JavaImpl implements MyInterface {
    public int getX() {
        return 1;
    }

    public int getY() {
        return 1;
    }

    public void setY(int value) {

    }

    // intentional override check for a val property
    public void setX(int value) {
        return value;
    }
}

// FILE: JavaDifferentReturnType.java
public abstract class JavaDifferentReturnType extends JavaList {
    // intentional different return type
    protected String foo() {
        return "";
    }
}