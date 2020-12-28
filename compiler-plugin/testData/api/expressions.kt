// WITH_RUNTIME
// TEST PROCESSOR: ExpressionsProcessor
// EXPECTED:
// init { .. }  =>  KSAnonymousInitializer
// var a = 0  =>  KSPropertyDeclaration
// 0  =>  KSConstantExpression
// val map = hashMapOf("one" to 1)  =>  KSPropertyDeclaration
// hashMapOf("one" to 1)  =>  KSCallExpression
// var one: Int by map  =>  KSPropertyDeclaration
// map  =>  KSCallExpression
// Builder(one).a().b().build()  =>  KSChainCallsExpression
// Builder(one)  =>  KSCallExpression
// a()  =>  KSCallExpression
// b()  =>  KSCallExpression
// build()  =>  KSCallExpression
// 1 * 2 + 3 - 4  =>  KSBinaryExpression
// 1 in 1..10  =>  KSBinaryExpression
// a++  =>  KSUnaryExpression
// --a  =>  KSUnaryExpression
// a as? String  =>  KSTypeCastExpression
// 1?.toString()  =>  KSCallExpression
// 2.toString()  =>  KSCallExpression
// when (val x = 5) { .. }  =>  KSWhenExpression
// val x = 5  =>  KSPropertyDeclaration
// 5  =>  KSConstantExpression
// is Any -> {}  =>  KSWhenExpression.Branch, isElse: false
// is Any  =>  <Unknown Expression>
// {}  =>  KSBlockExpression
// else -> print()  =>  KSWhenExpression.Branch, isElse: true
// print()  =>  KSCallExpression
// map.forEach { (_, value) -> .. }  =>  KSDslExpression
// { (_, value) -> .. }  =>  KSLambdaExpression
// if (value == null) { .. }  =>  KSIfExpression
// value == null  =>  KSBinaryExpression
// { .. }  =>  KSBlockExpression
// a = util.fetch(value)  =>  KSBinaryExpression
// require(util.check())  =>  KSCallExpression
// return  =>  KSJumpExpression
// throw Exception()  =>  KSJumpExpression
// loop@ run { .. }  =>  KSLabeledExpression
// run { .. }  =>  KSDslExpression
// { .. }  =>  KSLambdaExpression
// break@loop  =>  KSJumpExpression
// END
class Expressions {
    init {
        var a = 0
        val map = hashMapOf("one" to 1)
        var one: Int by map
        Builder(one).a().b().build()
        1 * 2 + 3 - 4
        1 in 1..10
        a++
        --a
        a as? String
        1?.toString()
        2.toString()
        when (val x = 5) {
            is Any -> {}
            else -> print()
        }
        map.forEach { (_, value) ->
            if (value == null) {
                a = util.fetch(value)
                require(util.check())
            }
        }
        return
        throw Exception()
        loop@ run {
            break@loop
        }
    }

    class Builder(int: Int) {
        fun a(): Builder = apply {  }
        fun b(): Builder = apply {  }
        fun build() = apply {  }
    }
}