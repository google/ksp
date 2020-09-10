// WITH_RUNTIME
// TEST PROCESSOR: ReferenceElementProcessor
// EXPECTED:
// KSClassifierReferenceImpl: Qualifier of B is A
// KSClassifierReferenceImpl: Qualifier of C is A
// KSClassifierReferenceImpl: Qualifier of Int is null
// KSClassifierReferenceImpl: Qualifier of String is null
// KSClassifierReferenceDescriptorImpl: Qualifier of B is A
// KSClassifierReferenceDescriptorImpl: Qualifier of C<Int> is A<String>
// KSClassifierReferenceDescriptorImpl: Qualifier of Int is null
// KSClassifierReferenceDescriptorImpl: Qualifier of String is null
// KSClassifierReferenceJavaImpl: Qualifier of H is J<String>
// KSClassifierReferenceJavaImpl: Qualifier of I is J
// KSClassifierReferenceJavaImpl: Qualifier of Object is null
// KSClassifierReferenceJavaImpl: Qualifier of String is null
// END

// FILE: reference.kt
class A<T1> {
    class B
    inner class C<T2>
}

val x: A.B = A.B()
val y: A<String>.C<Int> = A<String>().C<Int>()

// FILE: J.java
class J<T> {
    class H {
    }

    static class I {
    }
}

class K {
    J<String>.H x = null;
    J.I z = null;
}
