// WITH_RUNTIME
// TEST PROCESSOR: AllFunctionsProcessor
// EXPECTED:
// class: Foo
// bar(): kotlin.Boolean
// contains(kotlin.Number): kotlin.Boolean
// containsAll(kotlin.collections.Collection): kotlin.Boolean
// equals(kotlin.Any): kotlin.Boolean
// get(kotlin.Int): kotlin.Number
// hashCode(): kotlin.Int
// indexOf(kotlin.Number): kotlin.Int
// isEmpty(): kotlin.Boolean
// iterator(): kotlin.collections.Iterator
// javaListFun(): kotlin.collections.List
// javaStrFun(): kotlin.String
// lastIndexOf(kotlin.Number): kotlin.Int
// listIterator(): kotlin.collections.ListIterator
// listIterator(kotlin.Int): kotlin.collections.ListIterator
// subList(kotlin.Int,kotlin.Int): kotlin.collections.List
// toString(): kotlin.String
// END
// FILE: a.kt
abstract class Foo : C(), List<out Number> {
    override fun javaListFun(): List<Int> {
        throw java.lang.IllegalStateException()
    }

    fun bar(): Boolean {
        return false
    }
}

// FILE: C.java
class C {
    private void javaPrivateFun() {

    }

    protected Collection<Int> javaListFun() {
        return Arrays.asList(1,2,3)
    }

    public String javaStrFun() {
        return "str"
    }
}