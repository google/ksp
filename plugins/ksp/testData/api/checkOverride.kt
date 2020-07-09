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