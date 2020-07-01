// TEST PROCESSOR: CheckOverrideProcessor
// EXPECTED:
// KotlinList.get overrides JavaList.get: false
// KotlinList.foo overrides JavaList.foo: true
// KotlinList.fooo overrides JavaList.foo: false
// KotlinList.equals overrides JavaList.equals: true
// END
// FILE: a.kt

annotation class GetAnno
annotation class FooAnno
annotation class BarAnno


class KotlinList: JavaList() {
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