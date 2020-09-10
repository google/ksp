// TEST PROCESSOR: MapSignatureProcessor
// EXPECTED:
// LCls;
// (I)V
// I
// ()Ljava/lang/String;
// END

class Cls {
    constructor(a: Int) {
        this()
    }

    val a: Int = 1

    fun foo(): String { return "1" }
}