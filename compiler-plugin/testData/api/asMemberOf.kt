// WITH_RUNTIME
// TEST PROCESSOR: AsMemberOfProcessor
// EXPECTED:
// Child1!!
// intType: kotlin.Int!!
// baseTypeArg1: kotlin.Int!!
// baseTypeArg2: kotlin.String?
// typePair: kotlin.Pair!!<kotlin.String?, kotlin.Int!!>
// returnInt: () -> kotlin.Int!!
// returnArg1: () -> kotlin.Int!!
// returnArg1Nullable: () -> kotlin.Int?
// returnArg2: () -> kotlin.String?
// returnArg2Nullable: () -> kotlin.String?
// receiveArgs: (kotlin.Int?, kotlin.Int!!, kotlin.String?) -> kotlin.Unit!!
// receiveArgsPair: (kotlin.Pair!!<kotlin.Int!!, kotlin.String?>, kotlin.Pair?<kotlin.Int?, kotlin.String?>) -> kotlin.Unit!!
// functionArgType: <BaseTypeArg1: kotlin.Any?>(Base.functionArgType.BaseTypeArg1?) -> kotlin.String?
// functionArgTypeWithBounds: <in T: kotlin.Int!!>(Base.functionArgTypeWithBounds.T?) -> kotlin.String?
// Child2!!<kotlin.Any?>
// intType: kotlin.Int!!
// baseTypeArg1: kotlin.Any?
// baseTypeArg2: kotlin.Any?
// typePair: kotlin.Pair!!<kotlin.Any?, kotlin.Any?>
// returnInt: () -> kotlin.Int!!
// returnArg1: () -> kotlin.Any?
// returnArg1Nullable: () -> kotlin.Any?
// returnArg2: () -> kotlin.Any?
// returnArg2Nullable: () -> kotlin.Any?
// receiveArgs: (kotlin.Int?, kotlin.Any?, kotlin.Any?) -> kotlin.Unit!!
// receiveArgsPair: (kotlin.Pair!!<kotlin.Any?, kotlin.Any?>, kotlin.Pair?<kotlin.Any?, kotlin.Any?>) -> kotlin.Unit!!
// functionArgType: <BaseTypeArg1: kotlin.Any?>(Base.functionArgType.BaseTypeArg1?) -> kotlin.Any?
// functionArgTypeWithBounds: <in T: kotlin.Any?>(Base.functionArgTypeWithBounds.T?) -> kotlin.Any?
// Child2!!<kotlin.String!!>
// intType: kotlin.Int!!
// baseTypeArg1: kotlin.String!!
// baseTypeArg2: kotlin.String?
// typePair: kotlin.Pair!!<kotlin.String?, kotlin.String!!>
// returnInt: () -> kotlin.Int!!
// returnArg1: () -> kotlin.String!!
// returnArg1Nullable: () -> kotlin.String?
// returnArg2: () -> kotlin.String?
// returnArg2Nullable: () -> kotlin.String?
// receiveArgs: (kotlin.Int?, kotlin.String!!, kotlin.String?) -> kotlin.Unit!!
// receiveArgsPair: (kotlin.Pair!!<kotlin.String!!, kotlin.String?>, kotlin.Pair?<kotlin.String?, kotlin.String?>) -> kotlin.Unit!!
// functionArgType: <BaseTypeArg1: kotlin.Any?>(Base.functionArgType.BaseTypeArg1?) -> kotlin.String?
// functionArgTypeWithBounds: <in T: kotlin.String!!>(Base.functionArgTypeWithBounds.T?) -> kotlin.String?
// END
// FILE: Input.kt
open class Base<BaseTypeArg1, BaseTypeArg2> {
    val intType: Int = 0
    val baseTypeArg1: BaseTypeArg1 = TODO()
    val baseTypeArg2: BaseTypeArg2 = TODO()
    val typePair: Pair<BaseTypeArg2, BaseTypeArg1>  = TODO()
    fun returnInt():Int = TODO()
    fun returnArg1(): BaseTypeArg1 = TODO()
    fun returnArg1Nullable(): BaseTypeArg1? = TODO()
    fun returnArg2(): BaseTypeArg2 = TODO()
    fun returnArg2Nullable(): BaseTypeArg2? = TODO()
    fun receiveArgs(intArg:Int?, arg1: BaseTypeArg1, arg2:BaseTypeArg2):Unit = TODO()
    fun receiveArgsPair(
        pairs: Pair<BaseTypeArg1, BaseTypeArg2>,
        pairNullable: Pair<BaseTypeArg1?, BaseTypeArg2?>?,
    ):Unit = TODO()
    // intentional type argument name conflict here to ensure it does not get replaced by mistake
    fun <BaseTypeArg1> functionArgType(t:BaseTypeArg1?): BaseTypeArg2 = TODO()
    fun <in T: BaseTypeArg1> functionArgTypeWithBounds(t:T?): BaseTypeArg2 = TODO()
}

open class Child1 : Base<Int, String?>() {

}

open class Child2<ChildTypeArg1> : Base<ChildTypeArg1, ChildTypeArg1?>() {

}

val child2WithString : Child2<String> = TODO()