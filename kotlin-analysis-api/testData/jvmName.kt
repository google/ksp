// TEST PROCESSOR: JvmNameProcessor
// EXPECTED:
// (getX, setX), (getY, null)
// (getX, setX), (getY, null)
// END
// MODULE: lib
// FILE: Lib.kt
data class TestLibDataClass(var x: Int, val y: String)
// MODULE: main(lib)
// FILE: K.kt
data class TestDataClass(var x: Int, val y: String)
