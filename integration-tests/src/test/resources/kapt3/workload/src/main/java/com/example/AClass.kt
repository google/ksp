package com.example

import com.example.annotation.Builder
import hello.HELLO

@Builder
class AClass(private val a: Int, val b: String, val c: Double, val d: HELLO) {
    val p = "$a, $b, $c"
    fun foo() = HELLO()
    val hello = HELLO()
    var hello2: HELLO = HELLO()
        get() { return hello2 }
        private set
    class innerClass<T : HELLO>

    val generic = innerClass<HELLO>()
}
