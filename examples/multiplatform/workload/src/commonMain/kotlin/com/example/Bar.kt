package com.example

expect class Foo() {
    val foo: Boolean
    val bar: Boolean
    val baz: Boolean
}

class Bar {
    val baz = Foo().baz
}
