package com.example

import com.example.annotation.Builder
import HELLO

@Builder
class AClass(private val a: Int, val b: String, val c: Double, val d: HELLO) {
    val p = "$a, $b, $c, ${d.foo()}"
    fun foo() = p
}