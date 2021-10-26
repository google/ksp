package test
annotation class Anno

@Anno
class Foo() {
    val k = "123"
    var a : String = "123"
    val aaa : (Int) -> Int = { a -> 1 }
    fun bar(): Int {
        return 3
    }
}
