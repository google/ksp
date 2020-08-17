// WITH_RUNTIME
// TEST PROCESSOR: TypeAliasComparisonProcessor
// EXPECTED:
// [@Anno] [typealias F] = [@Anno] [typealias F] : true
// [@Anno] [typealias F] = String : true
// [@Anno] [typealias F] = [@Bnno] [typealias F] : true
// [@Anno] [typealias F] = String : true
// String = [@Anno] [typealias F] : true
// String = String : true
// String = [@Bnno] [typealias F] : true
// String = String : true
// [@Bnno] [typealias F] = [@Anno] [typealias F] : true
// [@Bnno] [typealias F] = String : true
// [@Bnno] [typealias F] = [@Bnno] [typealias F] : true
// [@Bnno] [typealias F] = String : true
// String = [@Anno] [typealias F] : true
// String = String : true
// String = [@Bnno] [typealias F] : true
// String = String : true
// END

annotation class Anno
annotation class Bnno

typealias F = String
typealias Foo = (@Anno F) -> String

fun bar(arg: @Bnno F) {
}
