// WITH_RUNTIME
// TEST PROCESSOR: TypeComparisonProcessor
// EXPECTED:
// (kotlin.String..kotlin.String?) ?= (kotlin.String..kotlin.String?) : true
// (kotlin.String..kotlin.String?) ?= String : true
// (kotlin.String..kotlin.String?) ?= String? : true
// String ?= (kotlin.String..kotlin.String?) : true
// String ?= String : true
// String ?= String? : false
// String? ?= (kotlin.String..kotlin.String?) : true
// String? ?= String : true
// String? ?= String? : true
// END

val j = java.lang.String.valueOf("")
val x: String = j
val y: String? = j
