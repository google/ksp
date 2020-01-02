// WITH_RUNTIME
// TEST PROCESSOR: MakeNullableProcessor
// EXPECTED:
// Int ?= Int : true
// Int ?= Int? : false
// Int ?= String : false
// Int ?= String? : false
// Int? ?= Int : true
// Int? ?= Int? : true
// Int? ?= String : false
// Int? ?= String? : false
// String ?= Int : false
// String ?= Int? : false
// String ?= String : true
// String ?= String? : false
// String? ?= Int : false
// String? ?= Int? : false
// String? ?= String : true
// String? ?= String? : true
// END

val x: String = ""
val y: Int? = 0
