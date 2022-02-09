package p1

interface Bar {
    fun s(): String
}
val bar: Foo = Foo()

annotation class MyAnnotation

fun main() {
    println("result: ${bar.s()}")
}
