package com.example

import com.kotlingen.MyKotlinClassBuilder
import com.javagen.MyJavaClassBuilder

fun main() {
    val builder = MyKotlinClassBuilder()
    val kotlinClass = builder.build()
    println(kotlinClass)

    val builder2 = MyJavaClassBuilder()
    val javaClass = builder.build()
    println(javaClass)
}
