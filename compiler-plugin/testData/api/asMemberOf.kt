// WITH_RUNTIME
// TEST PROCESSOR: AsMemberOfProcessor
// EXPECTED:
// Child1
// intType: kotlin.Int!!
// baseTypeArg1: kotlin.Int!!
// baseTypeArg2: kotlin.String?
// typePair: kotlin.Pair!!<kotlin.String?, kotlin.Int!!>
// END
// FILE: Input.kt
open class Base<BaseTypeArg1, BaseTypeArg2> {
    val intType: Int = 0
    val baseTypeArg1: BaseTypeArg1 = TODO()
    val baseTypeArg2: BaseTypeArg2 = TODO()
    val typePair: Pair<BaseTypeArg2, BaseTypeArg1>  = TODO()
}

open class Child1<ChildTypeArg1> : Base<Int, String?>() {

}
