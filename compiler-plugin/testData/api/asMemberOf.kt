// WITH_RUNTIME
// TEST PROCESSOR: AsMemberOfProcessor
// EXPECTED:
// Child1
// intType: kotlin.Int!!
// baseTypeArg1: kotlin.Int!!
// baseTypeArg2: kotlin.String?
// typePair: kotlin.Pair!!<kotlin.String?, kotlin.Int!!>
// getInt: kotlin.Int!!()
// getArg1: kotlin.Int!!()
// getArg1Nullable: kotlin.Int?()
// END
// FILE: Input.kt
open class Base<BaseTypeArg1, BaseTypeArg2> {
    val intType: Int = 0
    val baseTypeArg1: BaseTypeArg1 = TODO()
    val baseTypeArg2: BaseTypeArg2 = TODO()
    val typePair: Pair<BaseTypeArg2, BaseTypeArg1>  = TODO()
    fun getInt():Int = TODO()
    fun getArg1(): BaseTypeArg1 = TODO()
    fun getArg1Nullable(): BaseTypeArg1? = TODO()
}

open class Child1<ChildTypeArg1> : Base<Int, String?>() {

}
