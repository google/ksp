// TEST PROCESSOR: DefaultFunctionProcessor
// EXPECTED:
// funLiteral: false
// funWithBody: false
// emptyFun: true
// foo: false
// bar: true
// iterator: true
// equals: false
// interfaceProperty: isAbstract: true: isMutable: false
// interfaceVar: isAbstract: true: isMutable: true
// nonAbstractInterfaceProp: isAbstract: false: isMutable: false
// B: true
// parameterVal: isAbstract: false: isMutable: false
// parameterVar: isAbstract: false: isMutable: true
// abstractVar: isAbstract: true: isMutable: true
// abstractProperty: isAbstract: true: isMutable: false
// a: false
// END
// FILE: a.kt
interface KTInterface: Sequence<String> {
    fun funLiteral() = 1

    fun funWithBody(): Int {
        return 1
    }

    fun emptyFun()

    val interfaceProperty: String

    var interfaceVar: Int

    val nonAbstractInterfaceProp: Int
    get() = 1
}

abstract class B(val parameterVal: String, var parameterVar: String) {
    abstract var abstractVar: String
    abstract val abstractProperty: String
    val a: String = "str"
}

// FILE: C.java
interface C {
    default int foo() {
        return 1;
    }

    int bar()
}