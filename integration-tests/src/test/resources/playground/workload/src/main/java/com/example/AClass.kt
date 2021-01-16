package com.example

import com.example.annotation.Builder

@Builder
class AClass(private val a: Int, val b: String, val c: Double) {
    val p = "$a, $b, $c"
    fun foo() = p
}