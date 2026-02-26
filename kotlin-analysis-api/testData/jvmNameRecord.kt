// TEST PROCESSOR: JvmNameRecordProcessor
// EXPECTED:
// (x, null), (y, null)
// (x, null), (y, null)
// END
// MODULE: main
// FILE: TestRecordClass.kt
@JvmRecord
data class TestRecordClass(val x: Int, val y: String)
// FILE: TestLibRecordClass.kt
@JvmRecord
data class TestLibRecordClass(val x: Int, val y: String)
