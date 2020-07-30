// WITH_RUNTIME
// TEST PROCESSOR: TypeAliasProcessor
// EXPECTED:
// A = String
// A = String
// B = String
// C = A = String
// String
// String
// String
// END

typealias A = String
typealias B = String
typealias C = A
val a: A = ""
val b: B = ""
val c: C = ""
val d: String = ""
