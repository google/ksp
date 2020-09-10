// WITH_RUNTIME
// TEST PROCESSOR: FunctionTypeAliasProcessor
// EXPECTED:
// Function1<String, String> ?= Function1<String, String> : true / true
// Function1<String, String> ?= String : false / false
// Function1<String, String> ?= [typealias F] : false / false
// Function1<String, String> ?= [typealias Foo] : true / true
// String ?= Function1<String, String> : false / false
// String ?= String : true / true
// String ?= [typealias F] : true / true
// String ?= [typealias Foo] : false / false
// [typealias F] ?= Function1<String, String> : false / false
// [typealias F] ?= String : true / true
// [typealias F] ?= [typealias F] : true / true
// [typealias F] ?= [typealias Foo] : false / false
// [typealias Foo] ?= Function1<String, String> : true / true
// [typealias Foo] ?= String : false / false
// [typealias Foo] ?= [typealias F] : false / false
// [typealias Foo] ?= [typealias Foo] : true / true
// END

typealias F = String
val y: Foo = { it }
typealias Foo = (F) -> String
