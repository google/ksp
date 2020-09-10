// TEST PROCESSOR: VisibilityProcessor
// EXPECTED:
// publicFun: PUBLIC,visible in A, B, D: true, true, true
// packageFun: JAVA_PACKAGE,visible in A, B, D: true, false, true
// privateFun: PRIVATE,visible in A, B, D: false, false, false
// protectedFun: PROTECTED,visible in A, B, D: true, false, true
// END

// FILE: a.kt
annotation class TestA
annotation class TestB
annotation class TestD

@TestA
class A : C() {
}

@TestD
class D {}


// FILE: C.java
class C {
    public int publicFun() {
        return 1;
    }

    int packageFun() {
        return 1;
    }

    private int privateFun() {
        return 1;
    }

    protected int protectedFun() {
        return 1;
    }
}

// FILE: b.kt
package somePackage

import TestB

@TestB
class B
